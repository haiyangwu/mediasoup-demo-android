package org.mediasoup.droid.lib;

import static org.mediasoup.droid.lib.JsonUtils.jsonPut;
import static org.mediasoup.droid.lib.JsonUtils.toJsonObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.DataConsumer;
import org.mediasoup.droid.DataProducer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupException;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.socket.WebSocketTransport;
import org.protoojs.droid.Message;
import org.protoojs.droid.ProtooException;
import org.webrtc.AudioTrack;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

public class RoomClient extends RoomMessageHandler {

  public enum ConnectionState {
    // initial state.
    NEW,
    // connecting or reconnecting.
    CONNECTING,
    // connected.
    CONNECTED,
    // mClosed.
    CLOSED,
  }

  // Closed flag.
  private volatile boolean mClosed;
  // Android context.
  private final Context mContext;
  // PeerConnection util.
  private PeerConnectionUtils mPeerConnectionUtils;
  // Room mOptions.
  private final @NonNull RoomOptions mOptions;
  // Display name.
  private String mDisplayName;
  // Protoo URL.
  private String mProtooUrl;
  // mProtoo-client Protoo instance.
  private Protoo mProtoo;
  // mediasoup-client Device instance.
  private Device mMediasoupDevice;
  // mediasoup Transport for sending.
  private SendTransport mSendTransport;
  // mediasoup Transport for receiving.
  private RecvTransport mRecvTransport;
  // Local Audio Track for mic.
  private AudioTrack mLocalAudioTrack;
  // Local mic mediasoup Producer.
  private Producer mMicProducer;
  // local Video Track for cam.
  private VideoTrack mLocalVideoTrack;
  // Local cam mediasoup Producer.
  private Producer mCamProducer;
  // TODO(Haiyangwu): Local share mediasoup Producer.
  private Producer mShareProducer;
  // Local chat DataProducer.
  private DataProducer mChatDataProducer;
  // Local bot DataProducer.
  private DataProducer mBotDataProducer;
  // jobs worker handler.
  private Handler mWorkHandler;
  // main looper handler.
  private Handler mMainHandler;
  // Disposable Composite. used to cancel running
  private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
  // Share preferences
  private SharedPreferences mPreferences;

  public RoomClient(
      Context context, RoomStore roomStore, String roomId, String peerId, String displayName) {
    this(context, roomStore, roomId, peerId, displayName, false, false, null);
  }

  public RoomClient(
      Context context,
      RoomStore roomStore,
      String roomId,
      String peerId,
      String displayName,
      RoomOptions options) {
    this(context, roomStore, roomId, peerId, displayName, false, false, options);
  }

  public RoomClient(
      Context context,
      RoomStore roomStore,
      String roomId,
      String peerId,
      String displayName,
      boolean forceH264,
      boolean forceVP9,
      RoomOptions options) {
    super(roomStore);
    this.mContext = context.getApplicationContext();
    this.mOptions = options == null ? new RoomOptions() : options;
    this.mDisplayName = displayName;
    this.mClosed = false;
    this.mProtooUrl = UrlFactory.getProtooUrl(roomId, peerId, forceH264, forceVP9);

    this.mStore.setMe(peerId, displayName, this.mOptions.getDevice());
    this.mStore.setRoomUrl(roomId, UrlFactory.getInvitationLink(roomId, forceH264, forceVP9));
    this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);

    // init worker handler.
    HandlerThread handlerThread = new HandlerThread("worker");
    handlerThread.start();
    mWorkHandler = new Handler(handlerThread.getLooper());
    mMainHandler = new Handler(Looper.getMainLooper());
    mWorkHandler.post(() -> mPeerConnectionUtils = new PeerConnectionUtils());
  }

  @Async
  public void join() {
    Logger.d(TAG, "join() " + this.mProtooUrl);
    mStore.setRoomState(ConnectionState.CONNECTING);
    mWorkHandler.post(
        () -> {
          WebSocketTransport transport = new WebSocketTransport(mProtooUrl);
          mProtoo = new Protoo(transport, peerListener);
        });
  }

  @Async
  public void enableMic() {
    Logger.d(TAG, "enableMic()");
    mWorkHandler.post(this::enableMicImpl);
  }

  @Async
  public void disableMic() {
    Logger.d(TAG, "disableMic()");
    mWorkHandler.post(this::disableMicImpl);
  }

  @Async
  public void muteMic() {
    Logger.d(TAG, "muteMic()");
    mWorkHandler.post(this::muteMicImpl);
  }

  @Async
  public void unmuteMic() {
    Logger.d(TAG, "unmuteMic()");
    mWorkHandler.post(this::unmuteMicImpl);
  }

  @Async
  public void enableCam() {
    Logger.d(TAG, "enableCam()");
    mStore.setCamInProgress(true);
    mWorkHandler.post(
        () -> {
          enableCamImpl();
          mStore.setCamInProgress(false);
        });
  }

  @Async
  public void disableCam() {
    Logger.d(TAG, "disableCam()");
    mWorkHandler.post(this::disableCamImpl);
  }

  @Async
  public void changeCam() {
    Logger.d(TAG, "changeCam()");
    mStore.setCamInProgress(true);
    mWorkHandler.post(
        () ->
            mPeerConnectionUtils.switchCam(
                new CameraVideoCapturer.CameraSwitchHandler() {
                  @Override
                  public void onCameraSwitchDone(boolean b) {
                    mStore.setCamInProgress(false);
                  }

                  @Override
                  public void onCameraSwitchError(String s) {
                    Logger.w(TAG, "changeCam() | failed: " + s);
                    mStore.addNotify("error", "Could not change cam: " + s);
                    mStore.setCamInProgress(false);
                  }
                }));
  }

  @Async
  public void disableShare() {
    Logger.d(TAG, "disableShare()");
    // TODO(feature): share
  }

  @Async
  public void enableShare() {
    Logger.d(TAG, "enableShare()");
    // TODO(feature): share
  }

  @Async
  public void enableAudioOnly() {
    Logger.d(TAG, "enableAudioOnly()");
    mStore.setAudioOnlyInProgress(true);

    disableCam();
    mWorkHandler.post(
        () -> {
          for (ConsumerHolder holder : mConsumers.values()) {
            if (!"video".equals(holder.mConsumer.getKind())) {
              continue;
            }
            pauseConsumer(holder.mConsumer);
          }
          mStore.setAudioOnlyState(true);
          mStore.setAudioOnlyInProgress(false);
        });
  }

  @Async
  public void disableAudioOnly() {
    Logger.d(TAG, "disableAudioOnly()");
    mStore.setAudioOnlyInProgress(true);

    if (mCamProducer == null && mOptions.isProduce()) {
      enableCam();
    }
    mWorkHandler.post(
        () -> {
          for (ConsumerHolder holder : mConsumers.values()) {
            if (!"video".equals(holder.mConsumer.getKind())) {
              continue;
            }
            resumeConsumer(holder.mConsumer);
          }
          mStore.setAudioOnlyState(false);
          mStore.setAudioOnlyInProgress(false);
        });
  }

  @Async
  public void muteAudio() {
    Logger.d(TAG, "muteAudio()");
    mStore.setAudioMutedState(true);
    mWorkHandler.post(
        () -> {
          for (ConsumerHolder holder : mConsumers.values()) {
            if (!"audio".equals(holder.mConsumer.getKind())) {
              continue;
            }
            pauseConsumer(holder.mConsumer);
          }
        });
  }

  @Async
  public void unmuteAudio() {
    Logger.d(TAG, "unmuteAudio()");
    mStore.setAudioMutedState(false);
    mWorkHandler.post(
        () -> {
          for (ConsumerHolder holder : mConsumers.values()) {
            if (!"audio".equals(holder.mConsumer.getKind())) {
              continue;
            }
            resumeConsumer(holder.mConsumer);
          }
        });
  }

  @Async
  public void restartIce() {
    Logger.d(TAG, "restartIce()");
    mStore.setRestartIceInProgress(true);
    mWorkHandler.post(
        () -> {
          try {
            if (mSendTransport != null) {
              String iceParameters =
                  mProtoo.syncRequest(
                      "restartIce", req -> jsonPut(req, "transportId", mSendTransport.getId()));
              mSendTransport.restartIce(iceParameters);
            }
            if (mRecvTransport != null) {
              String iceParameters =
                  mProtoo.syncRequest(
                      "restartIce", req -> jsonPut(req, "transportId", mRecvTransport.getId()));
              mRecvTransport.restartIce(iceParameters);
            }
          } catch (Exception e) {
            e.printStackTrace();
            logError("restartIce() | failed:", e);
            mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
          }
          mStore.setRestartIceInProgress(false);
        });
  }

  @Async
  public void setMaxSendingSpatialLayer() {
    Logger.d(TAG, "setMaxSendingSpatialLayer()");
    // TODO(feature): layer
  }

  @Async
  public void setConsumerPreferredLayers(String spatialLayer) {
    Logger.d(TAG, "setConsumerPreferredLayers()");
    // TODO(feature): layer
  }

  @Async
  public void setConsumerPreferredLayers(
      String consumerId, String spatialLayer, String temporalLayer) {
    Logger.d(TAG, "setConsumerPreferredLayers()");
    // TODO: layer
  }

  @Async
  public void requestConsumerKeyFrame(String consumerId) {
    Logger.d(TAG, "requestConsumerKeyFrame()");
    mWorkHandler.post(
        () -> {
          try {
            mProtoo.syncRequest(
                "requestConsumerKeyFrame", req -> jsonPut(req, "consumerId", "consumerId"));
            mStore.addNotify("Keyframe requested for video consumer");
          } catch (ProtooException e) {
            e.printStackTrace();
            logError("restartIce() | failed:", e);
            mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
          }
        });
  }

  @Async
  public void enableChatDataProducer() {
    Logger.d(TAG, "enableChatDataProducer()");
    if (!mOptions.isUseDataChannel()) {
      return;
    }
    mWorkHandler.post(
        () -> {
          if (mChatDataProducer != null) {
            return;
          }
          try {
            DataProducer.Listener listener =
                new DataProducer.Listener() {
                  @Override
                  public void onOpen(DataProducer dataProducer) {
                    Logger.d(TAG, "chat DataProducer \"open\" event");
                  }

                  @Override
                  public void onClose(DataProducer dataProducer) {
                    Logger.e(TAG, "chat DataProducer \"close\" event");
                    mChatDataProducer = null;
                    mStore.addNotify("error", "Chat DataProducer closed");
                  }

                  @Override
                  public void onBufferedAmountChange(
                      DataProducer dataProducer, long sentDataSize) {}

                  @Override
                  public void onTransportClose(DataProducer dataProducer) {
                    mChatDataProducer = null;
                  }
                };
            mChatDataProducer =
                mSendTransport.produceData(
                    listener, "chat", "low", false, 1, 0, "{\"info\":\"my-chat-DataProducer\"}");
            mStore.addDataProducer(mChatDataProducer);
          } catch (Exception e) {
            Logger.e(TAG, "enableChatDataProducer() | failed:", e);
            mStore.addNotify("error", "Error enabling chat DataProducer: " + e.getMessage());
          }
        });
  }

  @Async
  public void enableBotDataProducer() {
    Logger.d(TAG, "enableBotDataProducer()");
    if (!mOptions.isUseDataChannel()) {
      return;
    }
    mWorkHandler.post(
        () -> {
          if (mBotDataProducer != null) {
            return;
          }
          try {
            DataProducer.Listener listener =
                new DataProducer.Listener() {
                  @Override
                  public void onOpen(DataProducer dataProducer) {
                    Logger.d(TAG, "bot DataProducer \"open\" event");
                  }

                  @Override
                  public void onClose(DataProducer dataProducer) {
                    Logger.e(TAG, "bot DataProducer \"close\" event");
                    mBotDataProducer = null;
                    mStore.addNotify("error", "Bot DataProducer closed");
                  }

                  @Override
                  public void onBufferedAmountChange(
                      DataProducer dataProducer, long sentDataSize) {}

                  @Override
                  public void onTransportClose(DataProducer dataProducer) {
                    mBotDataProducer = null;
                  }
                };
            mBotDataProducer =
                mSendTransport.produceData(
                    listener,
                    "bot",
                    "medium",
                    false,
                    -1,
                    2000,
                    "{\"info\":\"my-bot-DataProducer\"}");
            mStore.addDataProducer(mBotDataProducer);
          } catch (Exception e) {
            Logger.e(TAG, "enableBotDataProducer() | failed:", e);
            mStore.addNotify("error", "Error enabling bot DataProducer: " + e.getMessage());
          }
        });
  }

  @Async
  public void sendChatMessage(String txt) {
    Logger.d(TAG, "sendChatMessage()");
    mWorkHandler.post(
        () -> {
          if (mChatDataProducer == null) {
            mStore.addNotify("error", "No chat DataProduce");
            return;
          }

          try {
            mChatDataProducer.send(
                new DataChannel.Buffer(ByteBuffer.wrap(txt.getBytes("UTF-8")), false));
          } catch (Exception e) {
            Logger.e(TAG, "chat DataProducer.send() failed:", e);
            mStore.addNotify("error", "chat DataProducer.send() failed: " + e.getMessage());
          }
        });
  }

  @Async
  public void sendBotMessage(String txt) {
    Logger.d(TAG, "sendBotMessage()");
    mWorkHandler.post(
        () -> {
          if (mBotDataProducer == null) {
            mStore.addNotify("error", "No bot DataProduce");
            return;
          }

          try {
            mBotDataProducer.send(
                new DataChannel.Buffer(ByteBuffer.wrap(txt.getBytes("UTF-8")), false));
          } catch (Exception e) {
            Logger.e(TAG, "bot DataProducer.send() failed:", e);
            mStore.addNotify("error", "bot DataProducer.send() failed: " + e.getMessage());
          }
        });
  }

  @Async
  public void changeDisplayName(String displayName) {
    Logger.d(TAG, "changeDisplayName()");

    // Store in cookie.
    mPreferences.edit().putString("displayName", displayName).apply();

    mWorkHandler.post(
        () -> {
          try {
            mProtoo.syncRequest(
                "changeDisplayName", req -> jsonPut(req, "displayName", displayName));
            mDisplayName = displayName;
            mStore.setDisplayName(displayName);
            mStore.addNotify("Display name change");
          } catch (ProtooException e) {
            e.printStackTrace();
            logError("changeDisplayName() | failed:", e);
            mStore.addNotify("error", "Could not change display name: " + e.getMessage());

            // We need to refresh the component for it to render the previous
            // displayName again.
            mStore.setDisplayName(mDisplayName);
          }
        });
  }

  @Async
  public void getSendTransportRemoteStats() {
    Logger.d(TAG, "getSendTransportRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getRecvTransportRemoteStats() {
    Logger.d(TAG, "getRecvTransportRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getAudioRemoteStats() {
    Logger.d(TAG, "getAudioRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getVideoRemoteStats() {
    Logger.d(TAG, "getVideoRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getConsumerRemoteStats(String consumerId) {
    Logger.d(TAG, "getConsumerRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getChatDataProducerRemoteStats(String consumerId) {
    Logger.d(TAG, "getChatDataProducerRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getBotDataProducerRemoteStats() {
    Logger.d(TAG, "getBotDataProducerRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getDataConsumerRemoteStats(String dataConsumerId) {
    Logger.d(TAG, "getDataConsumerRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getSendTransportLocalStats() {
    Logger.d(TAG, "getSendTransportLocalStats()");
    // TODO(feature): stats
  }

  @Async
  public void getRecvTransportLocalStats() {
    Logger.d(TAG, "getRecvTransportLocalStats()");
    /// TODO(feature): stats
  }

  @Async
  public void getAudioLocalStats() {
    Logger.d(TAG, "getAudioLocalStats()");
    // TODO(feature): stats
  }

  @Async
  public void getVideoLocalStats() {
    Logger.d(TAG, "getVideoLocalStats()");
    // TODO(feature): stats
  }

  @Async
  public void getConsumerLocalStats(String consumerId) {
    Logger.d(TAG, "getConsumerLocalStats()");
    // TODO(feature): stats
  }

  @Async
  public void applyNetworkThrottle(String uplink, String downlink, String rtt, String secret) {
    Logger.d(TAG, "applyNetworkThrottle()");
    // TODO(feature): stats
  }

  @Async
  public void resetNetworkThrottle(boolean silent, String secret) {
    Logger.d(TAG, "applyNetworkThrottle()");
    // TODO(feature): stats
  }

  @Async
  public void close() {
    if (this.mClosed) {
      return;
    }
    this.mClosed = true;
    Logger.d(TAG, "close()");

    mWorkHandler.post(
        () -> {
          // Close mProtoo Protoo
          if (mProtoo != null) {
            mProtoo.close();
            mProtoo = null;
          }

          // dispose all transport and device.
          disposeTransportDevice();

          // dispose audio track.
          if (mLocalAudioTrack != null) {
            mLocalAudioTrack.setEnabled(false);
            mLocalAudioTrack.dispose();
            mLocalAudioTrack = null;
          }

          // dispose video track.
          if (mLocalVideoTrack != null) {
            mLocalVideoTrack.setEnabled(false);
            mLocalVideoTrack.dispose();
            mLocalVideoTrack = null;
          }

          // dispose peerConnection.
          mPeerConnectionUtils.dispose();

          // quit worker handler thread.
          mWorkHandler.getLooper().quit();
        });

    // dispose request.
    mCompositeDisposable.dispose();

    // Set room state.
    mStore.setRoomState(ConnectionState.CLOSED);
  }

  @WorkerThread
  private void disposeTransportDevice() {
    Logger.d(TAG, "disposeTransportDevice()");
    // Close mediasoup Transports.
    if (mSendTransport != null) {
      mSendTransport.close();
      mSendTransport.dispose();
      mSendTransport = null;
    }

    if (mRecvTransport != null) {
      mRecvTransport.close();
      mRecvTransport.dispose();
      mRecvTransport = null;
    }

    // dispose device.
    if (mMediasoupDevice != null) {
      mMediasoupDevice.dispose();
      mMediasoupDevice = null;
    }
  }

  private Protoo.Listener peerListener =
      new Protoo.Listener() {
        @Override
        public void onOpen() {
          mWorkHandler.post(() -> joinImpl());
        }

        @Override
        public void onFail() {
          mWorkHandler.post(
              () -> {
                mStore.addNotify("error", "WebSocket connection failed");
                mStore.setRoomState(ConnectionState.CONNECTING);
              });
        }

        @Override
        public void onRequest(
            @NonNull Message.Request request, @NonNull Protoo.ServerRequestHandler handler) {
          Logger.d(TAG, "onRequest() " + request.getData().toString());
          mWorkHandler.post(
              () -> {
                try {
                  switch (request.getMethod()) {
                    case "newConsumer":
                      {
                        onNewConsumer(request, handler);
                        break;
                      }
                    case "newDataConsumer":
                      {
                        onNewDataConsumer(request, handler);
                        break;
                      }
                    default:
                      {
                        handler.reject(403, "unknown protoo request.method " + request.getMethod());
                        Logger.w(TAG, "unknown protoo request.method " + request.getMethod());
                      }
                  }
                } catch (Exception e) {
                  Logger.e(TAG, "handleRequestError.", e);
                }
              });
        }

        @Override
        public void onNotification(@NonNull Message.Notification notification) {
          Logger.d(
              TAG,
              "onNotification() "
                  + notification.getMethod()
                  + ", "
                  + notification.getData().toString());
          mWorkHandler.post(
              () -> {
                try {
                  handleNotification(notification);
                } catch (Exception e) {
                  Logger.e(TAG, "handleNotification error.", e);
                }
              });
        }

        @Override
        public void onDisconnected() {
          mWorkHandler.post(
              () -> {
                mStore.addNotify("error", "WebSocket disconnected");
                mStore.setRoomState(ConnectionState.CONNECTING);

                // Close All Transports created by device.
                // All will reCreated After ReJoin.
                disposeTransportDevice();
              });
        }

        @Override
        public void onClose() {
          if (mClosed) {
            return;
          }
          mWorkHandler.post(
              () -> {
                if (mClosed) {
                  return;
                }
                close();
              });
        }
      };

  @WorkerThread
  private void joinImpl() {
    Logger.d(TAG, "joinImpl()");

    try {
      mMediasoupDevice = new Device();
      String routerRtpCapabilities = mProtoo.syncRequest("getRouterRtpCapabilities");
      mMediasoupDevice.load(routerRtpCapabilities, null);
      String rtpCapabilities = mMediasoupDevice.getRtpCapabilities();

      // Create mediasoup Transport for sending (unless we don't want to produce).
      if (mOptions.isProduce()) {
        createSendTransport();
      }

      // Create mediasoup Transport for sending (unless we don't want to consume).
      if (mOptions.isConsume()) {
        createRecvTransport();
      }

      final String sctpCapabilities =
          mOptions.isUseDataChannel() ? mMediasoupDevice.getSctpCapabilities() : "";

      // Join now into the room.
      String joinResponse =
          mProtoo.syncRequest(
              "join",
              req -> {
                jsonPut(req, "displayName", mDisplayName);
                jsonPut(req, "device", mOptions.getDevice().toJSONObject());
                jsonPut(req, "rtpCapabilities", toJsonObject(rtpCapabilities));
                jsonPut(req, "sctpCapabilities", sctpCapabilities);
              });

      mStore.setRoomState(ConnectionState.CONNECTED);
      mStore.addNotify("You are in the room!", 3000);

      JSONObject resObj = JsonUtils.toJsonObject(joinResponse);
      JSONArray peers = resObj.optJSONArray("peers");
      for (int i = 0; peers != null && i < peers.length(); i++) {
        JSONObject peer = peers.getJSONObject(i);
        mStore.addPeer(peer.optString("id"), peer);
      }

      // Enable mic/webcam.
      if (mOptions.isProduce()) {
        boolean canSendMic = mMediasoupDevice.canProduce("audio");
        boolean canSendCam = mMediasoupDevice.canProduce("video");
        mStore.setMediaCapabilities(canSendMic, canSendCam);
        mMainHandler.post(this::enableMic);
        mMainHandler.post(this::enableCam);
      }
    } catch (Exception e) {
      e.printStackTrace();
      logError("joinRoom() failed:", e);
      if (TextUtils.isEmpty(e.getMessage())) {
        mStore.addNotify("error", "Could not join the room, internal error");
      } else {
        mStore.addNotify("error", "Could not join the room: " + e.getMessage());
      }
      mMainHandler.post(this::close);
    }
  }

  @WorkerThread
  private void enableMicImpl() {
    Logger.d(TAG, "enableMicImpl()");
    try {
      if (mMicProducer != null) {
        return;
      }
      if (!mMediasoupDevice.isLoaded()) {
        Logger.w(TAG, "enableMic() | not loaded");
        return;
      }
      if (!mMediasoupDevice.canProduce("audio")) {
        Logger.w(TAG, "enableMic() | cannot produce audio");
        return;
      }
      if (mSendTransport == null) {
        Logger.w(TAG, "enableMic() | mSendTransport doesn't ready");
        return;
      }
      if (mLocalAudioTrack == null) {
        mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(mContext, "mic");
        mLocalAudioTrack.setEnabled(true);
      }
      mMicProducer =
          mSendTransport.produce(
              producer -> {
                Logger.e(TAG, "onTransportClose(), micProducer");
                if (mMicProducer != null) {
                  mStore.removeProducer(mMicProducer.getId());
                  mMicProducer = null;
                }
              },
              mLocalAudioTrack,
              null,
              null,
              null);
      mStore.addProducer(mMicProducer);
    } catch (MediasoupException e) {
      e.printStackTrace();
      logError("enableMic() | failed:", e);
      mStore.addNotify("error", "Error enabling microphone: " + e.getMessage());
      if (mLocalAudioTrack != null) {
        mLocalAudioTrack.setEnabled(false);
      }
    }
  }

  @WorkerThread
  private void disableMicImpl() {
    Logger.d(TAG, "disableMicImpl()");
    if (mMicProducer == null) {
      return;
    }

    mMicProducer.close();
    mStore.removeProducer(mMicProducer.getId());

    try {
      mProtoo.syncRequest("closeProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
    } catch (ProtooException e) {
      e.printStackTrace();
      mStore.addNotify("error", "Error closing server-side mic Producer: " + e.getMessage());
    }
    mMicProducer = null;
  }

  @WorkerThread
  private void muteMicImpl() {
    Logger.d(TAG, "muteMicImpl()");
    mMicProducer.pause();

    try {
      mProtoo.syncRequest("pauseProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
      mStore.setProducerPaused(mMicProducer.getId());
    } catch (ProtooException e) {
      e.printStackTrace();
      logError("muteMic() | failed:", e);
      mStore.addNotify("error", "Error pausing server-side mic Producer: " + e.getMessage());
    }
  }

  @WorkerThread
  private void unmuteMicImpl() {
    Logger.d(TAG, "unmuteMicImpl()");
    mMicProducer.resume();

    try {
      mProtoo.syncRequest(
          "resumeProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
      mStore.setProducerResumed(mMicProducer.getId());
    } catch (ProtooException e) {
      e.printStackTrace();
      logError("unmuteMic() | failed:", e);
      mStore.addNotify("error", "Error resuming server-side mic Producer: " + e.getMessage());
    }
  }

  @WorkerThread
  private void enableCamImpl() {
    Logger.d(TAG, "enableCamImpl()");
    try {
      if (mCamProducer != null) {
        return;
      }
      if (!mMediasoupDevice.isLoaded()) {
        Logger.w(TAG, "enableCam() | not loaded");
        return;
      }
      if (!mMediasoupDevice.canProduce("video")) {
        Logger.w(TAG, "enableCam() | cannot produce video");
        return;
      }
      if (mSendTransport == null) {
        Logger.w(TAG, "enableCam() | mSendTransport doesn't ready");
        return;
      }

      if (mLocalVideoTrack == null) {
        mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext, "cam");
        mLocalVideoTrack.setEnabled(true);
      }
      mCamProducer =
          mSendTransport.produce(
              producer -> {
                Logger.e(TAG, "onTransportClose(), camProducer");
                if (mCamProducer != null) {
                  mStore.removeProducer(mCamProducer.getId());
                  mCamProducer = null;
                }
              },
              mLocalVideoTrack,
              null,
              null,
              null);
      mStore.addProducer(mCamProducer);
    } catch (MediasoupException e) {
      e.printStackTrace();
      logError("enableWebcam() | failed:", e);
      mStore.addNotify("error", "Error enabling webcam: " + e.getMessage());
      if (mLocalVideoTrack != null) {
        mLocalVideoTrack.setEnabled(false);
      }
    }
  }

  @WorkerThread
  private void disableCamImpl() {
    Logger.d(TAG, "disableCamImpl()");
    if (mCamProducer == null) {
      return;
    }
    mCamProducer.close();
    mStore.removeProducer(mCamProducer.getId());

    try {
      mProtoo.syncRequest("closeProducer", req -> jsonPut(req, "producerId", mCamProducer.getId()));
    } catch (ProtooException e) {
      e.printStackTrace();
      mStore.addNotify("error", "Error closing server-side webcam Producer: " + e.getMessage());
    }
    mCamProducer = null;
  }

  @WorkerThread
  private void createSendTransport() throws ProtooException, JSONException, MediasoupException {
    Logger.d(TAG, "createSendTransport()");
    final String sctpCapabilities =
        mOptions.isUseDataChannel() ? mMediasoupDevice.getSctpCapabilities() : "";
    String res =
        mProtoo.syncRequest(
            "createWebRtcTransport",
            (req -> {
              jsonPut(req, "forceTcp", mOptions.isForceTcp());
              jsonPut(req, "producing", true);
              jsonPut(req, "consuming", false);
              jsonPut(req, "sctpCapabilities", sctpCapabilities);
            }));
    JSONObject info = new JSONObject(res);

    Logger.d(TAG, "device#createSendTransport() " + info);
    String id = info.optString("id");
    String iceParameters = info.optString("iceParameters");
    String iceCandidates = info.optString("iceCandidates");
    String dtlsParameters = info.optString("dtlsParameters");
    String sctpParameters = info.optString("sctpParameters");

    mSendTransport =
        mMediasoupDevice.createSendTransport(
            sendTransportListener,
            id,
            iceParameters,
            iceCandidates,
            dtlsParameters,
            sctpParameters);
  }

  @WorkerThread
  private void createRecvTransport() throws ProtooException, JSONException, MediasoupException {
    Logger.d(TAG, "createRecvTransport()");

    final String sctpCapabilities =
        mOptions.isUseDataChannel() ? mMediasoupDevice.getSctpCapabilities() : "";
    String res =
        mProtoo.syncRequest(
            "createWebRtcTransport",
            req -> {
              jsonPut(req, "forceTcp", mOptions.isForceTcp());
              jsonPut(req, "producing", false);
              jsonPut(req, "consuming", true);
              jsonPut(req, "sctpCapabilities", sctpCapabilities);
            });
    JSONObject info = new JSONObject(res);
    Logger.d(TAG, "device#createRecvTransport() " + info);
    String id = info.optString("id");
    String iceParameters = info.optString("iceParameters");
    String iceCandidates = info.optString("iceCandidates");
    String dtlsParameters = info.optString("dtlsParameters");
    String sctpParameters = info.optString("sctpParameters");

    mRecvTransport =
        mMediasoupDevice.createRecvTransport(
            recvTransportListener,
            id,
            iceParameters,
            iceCandidates,
            dtlsParameters,
            sctpParameters);
  }

  private SendTransport.Listener sendTransportListener =
      new SendTransport.Listener() {

        private String listenerTAG = TAG + "_SendTrans";

        @Override
        public String onProduce(
            Transport transport, String kind, String rtpParameters, String appData) {
          if (mClosed) {
            return "";
          }
          Logger.d(listenerTAG, "onProduce() ");
          String producerId =
              fetchProduceId(
                  req -> {
                    jsonPut(req, "transportId", transport.getId());
                    jsonPut(req, "kind", kind);
                    jsonPut(req, "rtpParameters", toJsonObject(rtpParameters));
                    jsonPut(req, "appData", appData);
                  });
          Logger.d(listenerTAG, "producerId: " + producerId);
          return producerId;
        }

        @Override
        public String onProduceData(
                Transport transport, String sctpStreamParameters, String label, String protocol, String appData) {
          if (mClosed) {
            return "";
          }
          Logger.d(listenerTAG, "onProduceData() ");
          String producerDataId =
                  fetchProduceDataId(
                          req -> {
                            jsonPut(req, "transportId", transport.getId());
                            jsonPut(req, "sctpStreamParameters", toJsonObject(sctpStreamParameters));
                            jsonPut(req, "label", label);
                            jsonPut(req, "protocol", protocol);
                            jsonPut(req, "appData", toJsonObject(appData));
                          });
          Logger.d(listenerTAG, "producerDataId: " + producerDataId);
          return producerDataId;
        }

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
          if (mClosed) {
            return;
          }
          Logger.d(listenerTAG + "_send", "onConnect()");
          mCompositeDisposable.add(
              mProtoo
                  .request(
                      "connectWebRtcTransport",
                      req -> {
                        jsonPut(req, "transportId", transport.getId());
                        jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters));
                      })
                  .subscribe(
                      d -> Logger.d(listenerTAG, "connectWebRtcTransport res: " + d),
                      t -> logError("connectWebRtcTransport for mSendTransport failed", t)));
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
          Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState);
          if ("connected".equals(connectionState)) {
              mMainHandler.post(()-> {
                 enableChatDataProducer();
                 enableBotDataProducer();
              });
          }
        }
      };

  private RecvTransport.Listener recvTransportListener =
      new RecvTransport.Listener() {

        private String listenerTAG = TAG + "_RecvTrans";

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
          if (mClosed) {
            return;
          }
          Logger.d(listenerTAG, "onConnect()");
          mCompositeDisposable.add(
              mProtoo
                  .request(
                      "connectWebRtcTransport",
                      req -> {
                        jsonPut(req, "transportId", transport.getId());
                        jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters));
                      })
                  .subscribe(
                      d -> Logger.d(listenerTAG, "connectWebRtcTransport res: " + d),
                      t -> logError("connectWebRtcTransport for mRecvTransport failed", t)));
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
          Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState);
        }
      };

  private String fetchProduceId(Protoo.RequestGenerator generator) {
    Logger.d(TAG, "fetchProduceId:()");
    try {
      String response = mProtoo.syncRequest("produce", generator);
      return new JSONObject(response).optString("id");
    } catch (ProtooException | JSONException e) {
      e.printStackTrace();
      logError("send produce request failed", e);
      return "";
    }
  }

  private String fetchProduceDataId(Protoo.RequestGenerator generator) {
    Logger.d(TAG, "fetchProduceDataId:()");
    try {
      String response = mProtoo.syncRequest("produceData", generator);
      return new JSONObject(response).optString("id");
    } catch (ProtooException | JSONException e) {
      e.printStackTrace();
      logError("send produce request failed", e);
      return "";
    }
  }

  private void logError(String message, Throwable throwable) {
    Logger.e(TAG, message, throwable);
  }

  private void onNewConsumer(Message.Request request, Protoo.ServerRequestHandler handler) {
    if (!mOptions.isConsume()) {
      handler.reject(403, "I do not want to consume");
      return;
    }
    try {
      JSONObject data = request.getData();
      String peerId = data.optString("peerId");
      String producerId = data.optString("producerId");
      String id = data.optString("id");
      String kind = data.optString("kind");
      String rtpParameters = data.optString("rtpParameters");
      String type = data.optString("type");
      String appData = data.optString("appData");
      boolean producerPaused = data.optBoolean("producerPaused");

      Consumer consumer =
          mRecvTransport.consume(
              c -> {
                mConsumers.remove(c.getId());
                Logger.w(TAG, "onTransportClose for consume");
              },
              id,
              producerId,
              kind,
              rtpParameters,
              appData);

      mConsumers.put(consumer.getId(), new ConsumerHolder(peerId, consumer));
      mStore.addConsumer(peerId, type, consumer, producerPaused);

      // We are ready. Answer the protoo request so the server will
      // resume this Consumer (which was paused for now if video).
      handler.accept();

      // If audio-only mode is enabled, pause it.
      if ("video".equals(consumer.getKind()) && mStore.getMe().getValue().isAudioOnly()) {
        pauseConsumer(consumer);
      }
    } catch (Exception e) {
      e.printStackTrace();
      logError("\"newConsumer\" request failed:", e);
      mStore.addNotify("error", "Error creating a Consumer: " + e.getMessage());
    }
  }

  private void onNewDataConsumer(Message.Request request, Protoo.ServerRequestHandler handler) {
    if (!mOptions.isConsume()) {
      handler.reject(403, "I do not want to consume");
      return;
    }
    if (!mOptions.isUseDataChannel()) {
      handler.reject(403, "I do not want DataChannels");
    }

    try {
      JSONObject data = request.getData();
      String peerId = data.optString("peerId");
      String dataProducerId = data.optString("dataProducerId");
      String id = data.optString("id");
      JSONObject sctpStreamParameters = data.optJSONObject("sctpStreamParameters");
      long streamId = sctpStreamParameters.optLong("streamId");
      String label = data.optString("label");
      String protocol = data.optString("protocol");
      String appData = data.optString("appData");

      DataConsumer.Listener listener =
          new DataConsumer.Listener() {
            @Override
            public void OnConnecting(DataConsumer dataConsumer) {}

            @Override
            public void OnOpen(DataConsumer dataConsumer) {
              Logger.d(TAG, "DataConsumer \"open\" event");
            }

            @Override
            public void OnClosing(DataConsumer dataConsumer) {}

            @Override
            public void OnClose(DataConsumer dataConsumer) {
              Logger.w(TAG, "DataConsumer \"close\" event");
              mDataConsumers.remove(dataConsumer.getId());
            }

            @Override
            public void OnMessage(DataConsumer dataConsumer, DataChannel.Buffer buffer) {
              try {
                JSONObject sctp = new JSONObject(dataConsumer.getSctpStreamParameters());
                Logger.w(
                    TAG,
                    "DataConsumer \"message\" event [streamId" + sctp.optInt("streamId") + "]");

                byte[] data = new byte[buffer.data.remaining()];
                buffer.data.get(data);
                String message = new String(data, "UTF-8");
                if ("chat".equals(dataConsumer.getLabel())) {
                  List<Peer> peerList = mStore.getPeers().getValue().getAllPeers();
                  Peer sendingPeer = null;
                  for (Peer item : peerList) {
                    if (item.getDataConsumers().contains(dataConsumer.getId())) {
                      sendingPeer = item;
                      break;
                    }
                  }
                  if (sendingPeer == null) {
                    Logger.w(TAG, "DataConsumer \"message\" from unknown peer");
                    return;
                  }
                  mStore.addNotifyMessage(sendingPeer.getDisplayName() + " says:", message);
                } else if ("bot".equals(dataConsumer.getLabel())) {
                  mStore.addNotifyMessage("Message from Bot:", message);
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            }

            @Override
            public void OnTransportClose(DataConsumer dataConsumer) {
              mDataConsumers.remove(dataConsumer.getId());
            }
          };
      DataConsumer dataConsumer =
          mRecvTransport.consumeData(
              listener, id, dataProducerId, streamId, label, protocol, appData);
      mDataConsumers.put(dataConsumer.getId(), new DataConsumerHolder(peerId, dataConsumer));
      mStore.addDataConsumer(peerId, dataConsumer);

      // We are ready. Answer the protoo request.
      handler.accept();

    } catch (Exception e) {
      e.printStackTrace();
      logError("\"newDataConsumer\" request failed:", e);
      mStore.addNotify("error", "Error creating a DataConsumer: " + e.getMessage());
    }
  }

  @WorkerThread
  private void pauseConsumer(Consumer consumer) {
    Logger.d(TAG, "pauseConsumer() " + consumer.getId());
    if (consumer.isPaused()) {
      return;
    }

    try {
      mProtoo.syncRequest("pauseConsumer", req -> jsonPut(req, "consumerId", consumer.getId()));
      consumer.pause();
      mStore.setConsumerPaused(consumer.getId(), "local");
    } catch (ProtooException e) {
      e.printStackTrace();
      logError("pauseConsumer() | failed:", e);
      mStore.addNotify("error", "Error pausing Consumer: " + e.getMessage());
    }
  }

  @WorkerThread
  private void resumeConsumer(Consumer consumer) {
    Logger.d(TAG, "resumeConsumer() " + consumer.getId());
    if (!consumer.isPaused()) {
      return;
    }

    try {
      mProtoo.syncRequest("resumeConsumer", req -> jsonPut(req, "consumerId", consumer.getId()));
      consumer.resume();
      mStore.setConsumerResumed(consumer.getId(), "local");
    } catch (Exception e) {
      e.printStackTrace();
      logError("resumeConsumer() | failed:", e);
      mStore.addNotify("error", "Error resuming Consumer: " + e.getMessage());
    }
  }
}

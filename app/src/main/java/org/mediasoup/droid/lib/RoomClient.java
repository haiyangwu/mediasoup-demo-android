package org.mediasoup.droid.lib;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.socket.WebSocketTransport;
import org.protoojs.droid.Message;
import org.webrtc.AudioTrack;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.CompositeDisposable;

import static org.mediasoup.droid.lib.JsonUtils.jsonPut;
import static org.mediasoup.droid.lib.JsonUtils.toJsonObject;

@SuppressWarnings("WeakerAccess")
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
  private boolean mClosed;
  // Android context.
  private final Context mContext;
  // Room mOptions.
  private final @NonNull RoomOptions mOptions;
  // Display name.
  private String mDisplayName;
  // TODO(Haiyangwu):Next expected dataChannel test number.
  private long mNextDataChannelTestNumber;
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
  // TODO(Haiyangwu): Local chat DataProducer.
  private Producer mChatDataProducer;
  // TODO(Haiyangwu): Local bot DataProducer.
  private Producer mBotDataProducer;
  // mediasoup Consumers.
  private Map<String, Consumer> mConsumers;
  // jobs worker handler.
  private Handler mWorkHandler;
  private Handler mMainHandler;
  // Disposable Composite. used to cancel running
  private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

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
    this.mConsumers = new ConcurrentHashMap<>();
    this.mProtooUrl = UrlFactory.getProtooUrl(roomId, peerId, forceH264, forceVP9);
    this.mConsumers = new HashMap<>();

    this.mStore.setMe(peerId, displayName, this.mOptions.getDevice());
    this.mStore.setRoomUrl(roomId, UrlFactory.getInvitationLink(roomId, forceH264, forceVP9));

    // support for selfSigned cert.
    UrlFactory.enableSelfSignedHttpClient();

    // init worker handler.
    HandlerThread handlerThread = new HandlerThread("worker");
    handlerThread.start();
    mWorkHandler = new Handler(handlerThread.getLooper());
    mMainHandler = new Handler(Looper.getMainLooper());
  }

  @MainThread
  public void join() {
    Logger.d(TAG, "join() " + this.mProtooUrl);
    mStore.setRoomState(ConnectionState.CONNECTING);
    WebSocketTransport transport = new WebSocketTransport(mProtooUrl);
    mProtoo = new Protoo(transport, peerListener);
  }

  @MainThread
  public void enableMic() {
    Logger.d(TAG, "enableMic()");
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
      mLocalAudioTrack = PeerConnectionUtils.createAudioTrack(mContext, "mic");
      mLocalAudioTrack.setEnabled(true);
    }
    mMicProducer =
        mSendTransport.produce(
            producer -> {
              Logger.e(TAG, "onTransportClose(), micProducer");
            },
            mLocalAudioTrack,
            null,
            null);
    mStore.addProducer(mMicProducer);
  }

  @MainThread
  public void disableMic() {
    Logger.d(TAG, "disableMic()");

    if (mMicProducer == null) {
      return;
    }

    mMicProducer.close();
    mStore.removeProducer(mMicProducer.getId());

    // TODO(HaiyangWu) : await
    mCompositeDisposable.add(
        mProtoo
            .request("closeProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()))
            .subscribe(
                res -> {},
                throwable ->
                    mStore.addNotify("Error closing server-side mic Producer: ", throwable)));

    mMicProducer = null;
  }

  @MainThread
  public void muteMic() {
    Logger.d(TAG, "muteMic()");
  }

  @MainThread
  public void unmuteMic() {
    Logger.d(TAG, "unmuteMic()");
    // TODO:
  }

  @MainThread
  public void enableCam() {
    Logger.d(TAG, "enableCam()");
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
      mLocalVideoTrack = PeerConnectionUtils.createVideoTrack(mContext, "cam");
      mLocalVideoTrack.setEnabled(true);
    }
    mCamProducer =
        mSendTransport.produce(
            producer -> {
              Logger.e(TAG, "onTransportClose(), camProducer");
            },
            mLocalVideoTrack,
            null,
            null);
    mStore.addProducer(mCamProducer);
  }

  @MainThread
  public void disableCam() {
    Logger.d(TAG, "disableCam()");
    // TODO:
  }

  @MainThread
  public void changeCam() {
    Logger.d(TAG, "changeCam()");
    mStore.setCamInProgress(true);
    PeerConnectionUtils.switchCam(
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
        });
  }

  @MainThread
  public void disableShare() {
    Logger.d(TAG, "disableShare()");
  }

  @MainThread
  public void enableShare() {
    Logger.d(TAG, "enableShare()");
  }

  @MainThread
  public void enableAudioOnly() {
    Logger.d(TAG, "enableAudioOnly()");
    // TODO:
  }

  @MainThread
  public void disableAudioOnly() {
    Logger.d(TAG, "disableAudioOnly()");
    // TODO:
  }

  @MainThread
  public void muteAudio() {
    Logger.d(TAG, "muteAudio()");
    // TODO:
  }

  @MainThread
  public void unmuteAudio() {
    Logger.d(TAG, "unmuteAudio()");
    // TODO:
  }

  @MainThread
  public void restartIce() {
    Logger.d(TAG, "restartIce()");
    // TODO:
  }

  @MainThread
  public void setMaxSendingSpatialLayer() {
    Logger.d(TAG, "setMaxSendingSpatialLayer()");
    // TODO:
  }

  @MainThread
  public void setConsumerPreferredLayers(String spatialLayer) {
    Logger.d(TAG, "setConsumerPreferredLayers()");
    // TODO:
  }

  @MainThread
  public void requestConsumerKeyFrame(
      String consumerId, String spatialLayer, String temporalLayer) {
    Logger.d(TAG, "requestConsumerKeyFrame()");
    // TODO:
  }

  @MainThread
  public void enableChatDataProducer() {
    Logger.d(TAG, "enableChatDataProducer()");
    // TODO:
  }

  @MainThread
  public void enableBotDataProducer() {
    Logger.d(TAG, "enableBotDataProducer()");
    // TODO:
  }

  @MainThread
  public void sendChatMessage(String txt) {
    Logger.d(TAG, "sendChatMessage()");
    // TODO:
  }

  @MainThread
  public void sendBotMessage(String txt) {
    Logger.d(TAG, "sendBotMessage()");
    // TODO:
  }

  @MainThread
  public void changeDisplayName(String displayName) {
    Logger.d(TAG, "changeDisplayName()");
    // TODO:
  }

  @MainThread
  public void getSendTransportRemoteStats() {
    Logger.d(TAG, "getSendTransportRemoteStats()");
    // TODO:
  }

  @MainThread
  public void close() {
    if (this.mClosed) {
      return;
    }
    this.mClosed = true;
    Logger.d(TAG, "close()");

    // Close mProtoo Protoo
    if (mProtoo != null) {
      mProtoo.close();
      mProtoo = null;
    }

    closeTransportAndDevice();

    // dispose track and media source.
    if (mLocalAudioTrack != null) {
      mLocalAudioTrack.dispose();
      mLocalAudioTrack = null;
    }
    if (mLocalVideoTrack != null) {
      mLocalVideoTrack.dispose();
      mLocalVideoTrack = null;
    }

    mStore.setRoomState(ConnectionState.CLOSED);
  }

  private void closeTransportAndDevice() {
    Logger.d(TAG, "closeTransportAndDevice()");
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

  public void dispose() {
    // quit worker handler thread.
    mWorkHandler.getLooper().quit();

    // dispose request.
    mCompositeDisposable.dispose();

    // dispose PeerConnectionUtils.
    PeerConnectionUtils.dispose();
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
          mWorkHandler.post(() -> handleRequest(request, handler));
        }

        @Override
        public void onNotification(@NonNull Message.Notification notification) {
          Logger.d(TAG, "onNotification() " + notification.getData().toString());
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
                closeTransportAndDevice();
              });
        }

        @Override
        public void onClose() {
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
      mMediasoupDevice.load(routerRtpCapabilities);
      String rtpCapabilities = mMediasoupDevice.getRtpCapabilities();

      // Create mediasoup Transport for sending (unless we don't want to produce).
      if (mOptions.isProduce()) {
        createSendTransport();
      }

      // Create mediasoup Transport for sending (unless we don't want to consume).
      if (mOptions.isConsume()) {
        createRecvTransport();
      }

      // Join now into the room.
      // TODO(HaiyangWu): Don't send our RTP capabilities if we don't want to consume.
      String joinResponse =
          mProtoo.syncRequest(
              "join",
              req -> {
                jsonPut(req, "displayName", mDisplayName);
                jsonPut(req, "device", mOptions.getDevice().toJSONObject());
                jsonPut(req, "rtpCapabilities", toJsonObject(rtpCapabilities));
                // TODO (HaiyangWu): add sctpCapabilities
                jsonPut(req, "sctpCapabilities", "");
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
        mWorkHandler.post(this::enableMic);
        mWorkHandler.post(this::enableCam);
      }
    } catch (Exception e) {
      e.printStackTrace();
      logError("joinRoom() failed:", e);
      mStore.addNotify("error", "Could not join the room: " + e.getMessage());
      mMainHandler.post(this::close);
    }
  }

  @WorkerThread
  private void createSendTransport() throws JSONException {
    Logger.d(TAG, "createSendTransport()");
    String res =
        mProtoo.syncRequest(
            "createWebRtcTransport",
            (req -> {
              jsonPut(req, "forceTcp", mOptions.isForceTcp());
              jsonPut(req, "producing", true);
              jsonPut(req, "consuming", false);
              // TODO: sctpCapabilities
              jsonPut(req, "sctpCapabilities", "");
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
            sendTransportListener, id, iceParameters, iceCandidates, dtlsParameters);
  }

  @WorkerThread
  private void createRecvTransport() throws JSONException {
    Logger.d(TAG, "createRecvTransport()");

    String res =
        mProtoo.syncRequest(
            "createWebRtcTransport",
            req -> {
              jsonPut(req, "forceTcp", mOptions.isForceTcp());
              jsonPut(req, "producing", false);
              jsonPut(req, "consuming", true);
              // TODO (HaiyangWu): add sctpCapabilities
              jsonPut(req, "sctpCapabilities", "");
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
            recvTransportListener, id, iceParameters, iceCandidates, dtlsParameters);
  }

  private SendTransport.Listener sendTransportListener =
      new SendTransport.Listener() {

        private String listenerTAG = TAG + "_SendTrans";

        @Override
        public String onProduce(
            Transport transport, String kind, String rtpParameters, String appData) {
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
        public void onConnect(Transport transport, String dtlsParameters) {
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
        }
      };

  private RecvTransport.Listener recvTransportListener =
      new RecvTransport.Listener() {

        private String listenerTAG = TAG + "_RecvTrans";

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
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
    StringBuffer result = new StringBuffer();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    mCompositeDisposable.add(
        mProtoo
            .request("produce", generator)
            .map(data -> toJsonObject(data).optString("id"))
            .subscribe(
                id -> {
                  result.append(id);
                  countDownLatch.countDown();
                },
                t -> {
                  logError("send produce request failed", t);
                  countDownLatch.countDown();
                }));
    try {
      // TODO(HaiyangWU): timeout or better solution ?
      countDownLatch.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return result.toString();
  }

  private void logError(String message, Throwable t) {
    Logger.e(TAG, message, t);
  }
}

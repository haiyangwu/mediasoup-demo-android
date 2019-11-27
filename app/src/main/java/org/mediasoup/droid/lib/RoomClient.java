package org.mediasoup.droid.lib;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.mediasoup.droid.lib.lv.RoomRepository;
import org.protoojs.droid.Message;
import org.mediasoup.droid.lib.socket.WebSocketTransport;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.mediasoup.droid.lib.JsonUtils.jsonPut;
import static org.mediasoup.droid.lib.JsonUtils.toJsonObject;

public class RoomClient {

  private static final String TAG = "RoomClient";

  public enum RoomState {
    // initial state.
    NEW,
    // connecting or reconnecting.
    CONNECTING,
    // connected.
    CONNECTED,
    // closed.
    CLOSED,
  }

  // Closed flag.
  private boolean closed;

  public static class RoomOptions {
    // Device info.
    private JSONObject device;
    // Whether we want to force RTC over TCP.
    private boolean forceTcp = false;
    // Whether we want to produce audio/video.
    private boolean produce = true;
    // Whether we should consume.
    private boolean consume = true;
    // Whether we want DataChannels.
    private boolean useDataChannel;

    public RoomOptions setDevice(JSONObject device) {
      this.device = device;
      return this;
    }

    public RoomOptions setForceTcp(boolean forceTcp) {
      this.forceTcp = forceTcp;
      return this;
    }

    public RoomOptions setProduce(boolean produce) {
      this.produce = produce;
      return this;
    }

    public RoomOptions setConsume(boolean consume) {
      this.consume = consume;
      return this;
    }

    public RoomOptions setUseDataChannel(boolean useDataChannel) {
      this.useDataChannel = useDataChannel;
      return this;
    }
  }

  // Android context.
  private final Context context;
  // Room options.
  private final @NonNull RoomOptions options;
  // Display name.
  private String displayName;
  // TODO(Haiyangwu):Next expected dataChannel test number.
  private long nextDataChannelTestNumber;
  // Protoo URL.
  private String protooUrl;
  // protoo-client Peer instance.
  private Peer protoo;
  // mediasoup-client Device instance.
  private Device mediasoupDevice;
  // mediasoup Transport for sending.
  private SendTransport sendTransport;
  // mediasoup Transport for receiving.
  private RecvTransport recvTransport;
  // Local Audio Track for mic.
  private AudioTrack localAudioTrack;
  // Local mic mediasoup Producer.
  private Producer micProducer;
  // local Video Track for cam.
  private VideoTrack localVideoTrack;
  // Local cam mediasoup Producer.
  private Producer camProducer;
  // TODO(Haiyangwu): Local share mediasoup Producer.
  private Producer shareProducer;
  // TODO(Haiyangwu): Local chat DataProducer.
  private Producer chatDataProducer;
  // TODO(Haiyangwu): Local bot DataProducer.
  private Producer botDataProducer;
  // mediasoup Consumers.
  private Map<String, Consumer> consumers;
  // jobs worker handler.
  private Handler workHandler;
  // Stored Room States.
  private final @NonNull RoomRepository roomRepository;

  public RoomClient(Context context, String roomId, String peerId, String displayName) {
    this(context, roomId, peerId, displayName, false, false, null);
  }

  public RoomClient(
      Context context, String roomId, String peerId, String displayName, RoomOptions options) {
    this(context, roomId, peerId, displayName, false, false, options);
  }

  public RoomClient(
      Context context,
      String roomId,
      String peerId,
      String displayName,
      boolean forceH264,
      boolean forceVP9,
      RoomOptions options) {
    this.context = context.getApplicationContext();
    this.options = options == null ? new RoomOptions() : options;
    this.displayName = displayName;
    this.closed = false;
    this.consumers = new ConcurrentHashMap<>();
    this.protooUrl = UrlFactory.getProtooUrl(roomId, peerId, forceH264, forceVP9);
    this.consumers = new HashMap<>();
    this.roomRepository = RoomRepository.getInstance();
    this.roomRepository.setMe(peerId, displayName, this.options.device);
    this.roomRepository.setRoomUrl(
        roomId, UrlFactory.getInvitationLink(roomId, forceH264, forceVP9));

    // support for selfSigned cert.
    UrlFactory.enableSelfSignedHttpClient();

    // init worker handler.
    HandlerThread handlerThread = new HandlerThread("worker");
    handlerThread.start();
    workHandler = new Handler(handlerThread.getLooper());
  }

  @MainThread
  public void join() {
    Logger.d(TAG, "join() " + this.protooUrl);
    roomRepository.setRoomState(RoomState.CONNECTING);
    WebSocketTransport transport = new WebSocketTransport(protooUrl);
    protoo = new Peer(transport, peerListener);
  }

  @MainThread
  private void enableMic() {
    if (!mediasoupDevice.isLoaded()) {
      Logger.w(TAG, "enableMic() | not loaded");
      return;
    }
    if (!mediasoupDevice.canProduce("audio")) {
      Logger.w(TAG, "enableMic() | cannot produce audio");
      return;
    }
    if (sendTransport == null) {
      Logger.w(TAG, "enableMic() | sendTransport doesn't ready");
      return;
    }
    if (localAudioTrack == null) {
      localAudioTrack = PeerConnectionUtils.createAudioTrack(context, "mic");
      localAudioTrack.setEnabled(true);
    }
    micProducer =
        sendTransport.produce(
            producer -> {
              Logger.w(TAG, "onTransportClose()");
            },
            localAudioTrack,
            null,
            null);
  }

  @MainThread
  private void enableCam() {
    if (!mediasoupDevice.isLoaded()) {
      Logger.w(TAG, "enableCam() | not loaded");
      return;
    }
    if (!mediasoupDevice.canProduce("video")) {
      Logger.w(TAG, "enableCam() | cannot produce video");
      return;
    }
    if (sendTransport == null) {
      Logger.w(TAG, "enableCam() | sendTransport doesn't ready");
      return;
    }
    if (localVideoTrack == null) {
      localVideoTrack = PeerConnectionUtils.createVideoTrack(context, "cam");
      localVideoTrack.setEnabled(true);
    }
    camProducer =
        sendTransport.produce(
            producer -> {
              Logger.w(TAG, "onTransportClose()");
            },
            localVideoTrack,
            null,
            null);
  }

  @MainThread
  public void close() {
    if (this.closed) {
      return;
    }
    this.closed = true;
    Logger.d(TAG, "close()");

    // Close protoo Peer
    if (protoo != null) {
      protoo.close();
    }

    // Close mediasoup Transports.
    if (sendTransport != null) {
      sendTransport.close();
    }
    if (recvTransport != null) {
      recvTransport.close();
    }

    // dispose device.
    if (mediasoupDevice != null) {
      mediasoupDevice.dispose();
    }

    // quit worker handler thread.
    workHandler.getLooper().quit();

    roomRepository.setRoomState(RoomState.CLOSED);

    // dispose track and media source.
    if (localAudioTrack != null) {
      localAudioTrack.dispose();
      localAudioTrack = null;
    }
    if (localVideoTrack != null) {
      localVideoTrack.dispose();
      localVideoTrack = null;
    }
    PeerConnectionUtils.dispose();
  }

  private Peer.Listener peerListener =
      new Peer.Listener() {
        @Override
        public void onOpen() {
          workHandler.post(() -> joinImpl());
        }

        @Override
        public void onFail() {
          roomRepository.addNotify("error", "WebSocket connection failed");
          roomRepository.setRoomState(RoomState.CONNECTING);
        }

        @Override
        public void onRequest(
            @NonNull Message.Request request, @NonNull Peer.ServerRequestHandler handler) {
          Logger.d(TAG, "onRequest() " + request.getData().toString());
          handleRequest(request, handler);
        }

        @Override
        public void onNotification(@NonNull Message.Notification notification) {
          Logger.d(TAG, "onNotification() " + notification.getData().toString());
          handleNotification(notification);
        }

        @Override
        public void onDisconnected() {
          roomRepository.addNotify("error", "WebSocket disconnected");
          roomRepository.setRoomState(RoomState.CONNECTING);
        }

        @Override
        public void onClose() {
          if (closed) {
            return;
          }
          close();
        }
      };

  private void handleRequest(Message.Request request, Peer.ServerRequestHandler handler) {
    // TODO (HaiyangWu): handle request msg
    switch (request.getMethod()) {
      case "newConsumer":
        {
          break;
        }
      case "newDataConsumer":
        {
          break;
        }
    }
  }

  private void handleNotification(Message.Notification notification) {
    // TODO (HaiyangWu): handle notification msg
  }

  @WorkerThread
  private void joinImpl() {
    Logger.d(TAG, "joinImpl()");
    roomRepository.setRoomState(RoomState.CONNECTED);
    mediasoupDevice = new Device();
    protoo
        .request("getRouterRtpCapabilities")
        .map(
            data -> {
              mediasoupDevice.load(data);
              return mediasoupDevice.getRtpCapabilities();
            })
        .flatMap(
            rtpCapabilities -> {
              JSONObject deviceInfo = options.device;
              if (deviceInfo == null) {
                deviceInfo = new JSONObject();
                jsonPut(deviceInfo, "flag", "android");
                jsonPut(deviceInfo, "name", "Android " + Build.DEVICE);
                jsonPut(deviceInfo, "version", Build.VERSION.CODENAME);
              }

              JSONObject request = new JSONObject();
              jsonPut(request, "displayName", displayName);
              jsonPut(request, "device", deviceInfo);
              jsonPut(request, "rtpCapabilities", toJsonObject(rtpCapabilities));
              // TODO (HaiyangWu): add sctpCapabilities
              jsonPut(request, "sctpCapabilities", "");
              return protoo.request("join", request);
            })
        .doOnError(
            t -> {
              logError("_joinRoom() failed", t);
              roomRepository.addNotify("error", "Could not join the room: " + t.getMessage());
              this.close();
            })
        .subscribe(
            res -> {
              roomRepository.setRoomState(RoomState.CONNECTED);
              roomRepository.addNotify("You are in the room!", 3000);

              JSONObject resObj = JsonUtils.toJsonObject(res);
              JSONArray peers = resObj.optJSONArray("peers");
              for (int i = 0; peers != null && i < peers.length(); i++) {
                JSONObject peer = peers.getJSONObject(i);
                roomRepository.addPeer(peer.optString("id"), peer);
              }
              if (options.produce) {
                boolean canSendMic = mediasoupDevice.canProduce("audio");
                boolean canSendCam = mediasoupDevice.canProduce("video");
                roomRepository.setMediaCapabilities(canSendMic, canSendCam);

                workHandler.post(this::createSendTransport);
              }
              if (options.consume) {
                workHandler.post(this::createRecvTransport);
              }
            });
  }

  @WorkerThread
  private void createSendTransport() {
    Logger.d(TAG, "createSendTransport()");
    JSONObject request = new JSONObject();
    jsonPut(request, "forceTcp", options.forceTcp);
    jsonPut(request, "producing", true);
    jsonPut(request, "consuming", false);
    jsonPut(request, "sctpCapabilities", "");

    protoo
        .request("createWebRtcTransport", request)
        .map(JSONObject::new)
        .doOnError(t -> logError("createWebRtcTransport for sendTransport failed", t))
        .subscribe(info -> workHandler.post(() -> createLocalSendTransport(info)));
  }

  @WorkerThread
  private void createLocalSendTransport(JSONObject transportInfo) {
    Logger.d(TAG, "createLocalSendTransport() " + transportInfo);
    String id = transportInfo.optString("id");
    String iceParameters = transportInfo.optString("iceParameters");
    String iceCandidates = transportInfo.optString("iceCandidates");
    String dtlsParameters = transportInfo.optString("dtlsParameters");
    String sctpParameters = transportInfo.optString("sctpParameters");

    sendTransport =
        mediasoupDevice.createSendTransport(
            sendTransportListener, id, iceParameters, iceCandidates, dtlsParameters);

    if (options.produce) {
      workHandler.post(this::enableMic);
      workHandler.post(this::enableCam);
    }
  }

  @WorkerThread
  private void createRecvTransport() {
    Logger.d(TAG, "createRecvTransport()");
    JSONObject request = new JSONObject();
    jsonPut(request, "forceTcp", options.forceTcp);
    jsonPut(request, "producing", false);
    jsonPut(request, "consuming", true);
    jsonPut(request, "sctpCapabilities", "");

    protoo
        .request("createWebRtcTransport", request)
        .map(JSONObject::new)
        .doOnError(t -> logError("createWebRtcTransport for recvTransport failed", t))
        .subscribe(info -> workHandler.post(() -> createLocalRecvTransport(info)));
  }

  @WorkerThread
  private void createLocalRecvTransport(JSONObject transportInfo) {
    Logger.d(TAG, "createLocalRecvTransport() " + transportInfo);
    String id = transportInfo.optString("id");
    String iceParameters = transportInfo.optString("iceParameters");
    String iceCandidates = transportInfo.optString("iceCandidates");
    String dtlsParameters = transportInfo.optString("dtlsParameters");
    String sctpParameters = transportInfo.optString("sctpParameters");

    recvTransport =
        mediasoupDevice.createRecvTransport(
            recvTransportListener, id, iceParameters, iceCandidates, dtlsParameters);
  }

  private SendTransport.Listener sendTransportListener =
      new SendTransport.Listener() {

        private String listenerTAG = TAG + "_SendTrans";

        @Override
        public String onProduce(
            Transport transport, String kind, String rtpParameters, String appData) {
          Logger.d(listenerTAG, "onProduce() ");

          JSONObject request = new JSONObject();
          jsonPut(request, "transportId", transport.getId());
          jsonPut(request, "kind", kind);
          jsonPut(request, "rtpParameters", toJsonObject(rtpParameters));
          jsonPut(request, "appData", appData);

          Logger.d(listenerTAG, "send produce request with " + request.toString());
          String producerId = fetchProduceId(request);
          Logger.d(listenerTAG, "producerId: " + producerId);
          return producerId;
        }

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
          Logger.d(listenerTAG + "_send", "onConnect()");
          JSONObject request = new JSONObject();
          jsonPut(request, "transportId", transport.getId());
          jsonPut(request, "dtlsParameters", toJsonObject(dtlsParameters));
          protoo
              .request("connectWebRtcTransport", request)
              // TODO (HaiyangWu): handle error
              .doOnError(t -> logError("connectWebRtcTransport for sendTransport failed", t))
              .subscribe(
                  data -> {
                    Logger.d(listenerTAG, "connectWebRtcTransport res: " + data);
                  });
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
          JSONObject request = new JSONObject();
          jsonPut(request, "transportId", transport.getId());
          jsonPut(request, "dtlsParameters", toJsonObject(dtlsParameters));
          protoo
              .request("connectWebRtcTransport", request)
              // TODO (HaiyangWu): handle error
              .doOnError(t -> logError("connectWebRtcTransport for recvTransport failed", t))
              .subscribe(
                  data -> {
                    Logger.d(listenerTAG, "connectWebRtcTransport res: " + data);
                  });
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
          Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState);
        }
      };

  private String fetchProduceId(JSONObject request) {
    StringBuffer result = new StringBuffer();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    protoo
        .request("produce", request)
        .map(data -> toJsonObject(data).optString("id"))
        .doOnError(e -> logError("send produce request failed", e))
        .subscribe(
            id -> {
              result.append(id);
              countDownLatch.countDown();
            });
    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return result.toString();
  }

  private void logError(String message, Throwable t) {
    Logger.e(TAG, message, t);
  }
}

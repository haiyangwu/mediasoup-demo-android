package org.mediasoup.droid.lib;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.protoojs.droid.Message;
import org.mediasoup.droid.lib.socket.WebSocketTransport;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.mediasoup.droid.lib.JsonUtils.jsonPut;
import static org.mediasoup.droid.lib.JsonUtils.toJsonObject;

public class RoomClient {

  private static final String TAG = "RoomClient";

  public enum RoomState {
    CONNECTING,
    CONNECTED,
    CLOSED,
  }

  // Closed flag.
  private boolean closed;
  // TODO (HaiyangWu): add ConfigBuilder
  // Device info.
  private String device;
  // Display name.
  private String displayName;
  // Whether we want to force RTC over TCP.
  private boolean forceTcp = false;
  // Whether we want to produce audio/video.
  private boolean produce = true;
  // Whether we should consume.
  private boolean consume = true;
  // Whether we want DataChannels.
  private boolean useDataChannel;
  // Next expected dataChannel test number.
  private long nextDataChannelTestNumber;

  private String protooUrl;
  private Peer protoo;

  private Device mediasoupDevice;
  private SendTransport sendTransport;
  private RecvTransport recvTransport;

  private AudioTrack localAudioTrack;
  private VideoTrack localVideoTrack;

  private Map<String, Consumer> consumers;

  private Handler workHandler;

  public RoomClient(String roomId, String peerId, String displayName) {
    this(roomId, peerId, displayName, false, false);
  }

  public RoomClient(
      String roomId, String peerId, String displayName, boolean forceH264, boolean forceVP9) {
    this.displayName = displayName;
    this.closed = false;
    this.consumers = new ConcurrentHashMap<>();
    this.protooUrl = UrlFactory.getProtooUrl(roomId, peerId, forceH264, forceVP9);

    // for selfSigned.
    UrlFactory.enableSelfSignedHttpClient();

    HandlerThread handlerThread = new HandlerThread("worker");
    handlerThread.start();
    workHandler = new Handler(handlerThread.getLooper());
  }

  @MainThread
  public void join() {
    Logger.d(TAG, "join() " + this.protooUrl);

    WebSocketTransport transport = new WebSocketTransport(protooUrl);
    protoo = new Peer(transport, peerListener);
  }

  @MainThread
  public void enableMic(Context context) {
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
    workHandler.post(this::enableMicImpl);
  }

  @MainThread
  public void enableCam(Context context) {
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
    workHandler.post(this::enableCameraImpl);
  }

  @WorkerThread
  private void enableMicImpl() {
    sendTransport.produce(
        producer -> {
          Logger.w(TAG, "onTransportClose()");
        },
        localAudioTrack,
        null,
        null);
  }

  @WorkerThread
  private void enableCameraImpl() {
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
    if (sendTransport != null) {
      sendTransport.close();
    }
    if (recvTransport != null) {
      recvTransport.close();
    }
    if (mediasoupDevice != null) {
      mediasoupDevice.dispose();
    }
    workHandler.getLooper().quit();
  }

  private Peer.Listener peerListener =
      new Peer.Listener() {
        @Override
        public void onOpen() {
          mediasoupDevice = new Device();
          workHandler.post(() -> joinImpl());
        }

        @Override
        public void onFail() {
          // TODO (HaiyangWu): notify state
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
          // TODO (HaiyangWu): notify state
        }

        @Override
        public void onClose() {
          // TODO (HaiyangWu): notify state
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
    protoo
        .request("getRouterRtpCapabilities")
        .map(
            data -> {
              mediasoupDevice.load(data);
              return mediasoupDevice.getRtpCapabilities();
            })
        .flatMap(
            rtpCapabilities -> {
              JSONObject deviceInfo = new JSONObject();
              jsonPut(deviceInfo, "flag", "android");
              jsonPut(deviceInfo, "name", "Android " + Build.DEVICE);
              jsonPut(deviceInfo, "version", Build.VERSION.CODENAME);

              JSONObject request = new JSONObject();
              jsonPut(request, "displayName", displayName);
              jsonPut(request, "device", deviceInfo);
              jsonPut(request, "rtpCapabilities", toJsonObject(rtpCapabilities));
              // TODO (HaiyangWu): add sctpCapabilities
              jsonPut(request, "sctpCapabilities", "");
              return protoo.request("join", request);
            })
        .doOnError(t -> doOnError("_joinRoom() failed", t))
        .subscribe(
            peers -> {
              Logger.d(TAG, "peers: " + peers);
              // TODO (HaiyangWu): notify peers
              if (produce) {
                workHandler.post(this::createSendTransport);
              }
              if (consume) {
                workHandler.post(this::createRecvTransport);
              }
            });
  }

  @WorkerThread
  private void createSendTransport() {
    Logger.d(TAG, "createSendTransport()");
    JSONObject request = new JSONObject();
    jsonPut(request, "forceTcp", forceTcp);
    jsonPut(request, "producing", true);
    jsonPut(request, "consuming", false);
    jsonPut(request, "sctpCapabilities", "");

    protoo
        .request("createWebRtcTransport", request)
        .map(JSONObject::new)
        .doOnError(t -> doOnError("createWebRtcTransport for sendTransport failed", t))
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
  }

  @WorkerThread
  private void createRecvTransport() {
    Logger.d(TAG, "createRecvTransport()");
    JSONObject request = new JSONObject();
    jsonPut(request, "forceTcp", forceTcp);
    jsonPut(request, "producing", false);
    jsonPut(request, "consuming", true);
    jsonPut(request, "sctpCapabilities", "");

    protoo
        .request("createWebRtcTransport", request)
        .map(JSONObject::new)
        .doOnError(t -> doOnError("createWebRtcTransport for recvTransport failed", t))
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
              .doOnError(t -> doOnError("connectWebRtcTransport for sendTransport failed", t))
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
              .doOnError(t -> doOnError("connectWebRtcTransport for recvTransport failed", t))
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
        .doOnError(e -> doOnError("send produce request failed", e))
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

  private void doOnError(String message, Throwable t) {
    Logger.e(TAG, message, t);
  }
}

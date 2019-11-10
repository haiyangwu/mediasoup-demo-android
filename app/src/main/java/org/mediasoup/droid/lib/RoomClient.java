package org.mediasoup.droid.lib;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import org.json.JSONObject;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.protoojs.droid.Message;
import org.protoojs.droid.Peer;
import org.protoojs.droid.transports.WebSocketTransport;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.concurrent.CountDownLatch;

import static org.mediasoup.droid.lib.JsonUtils.jsonPut;
import static org.mediasoup.droid.lib.JsonUtils.toJsonObject;

public class RoomClient {

  private static final String TAG = "RoomClient";

  private String roomId;
  private String peerId;
  private String device;
  private String displayName;

  private String protooUrl;
  private Peer protoo;

  private Device mediasoupDevice;
  private SendTransport sendTransport;
  private AudioTrack audioTrack;
  private VideoTrack videoTrack;

  private Handler workHandler;

  public RoomClient(String roomId, String peerId, String displayName) {
    this(roomId, peerId, displayName, false, false);
  }

  public RoomClient(
      String roomId, String peerId, String displayName, boolean forceH264, boolean forceVP9) {
    this.roomId = roomId;
    this.peerId = peerId;
    this.displayName = displayName;
    this.protooUrl = UrlFactory.getProtooUrl(roomId, peerId, forceH264, forceVP9);
    HandlerThread handlerThread = new HandlerThread("worker");
    handlerThread.start();

    UrlFactory.enableSelfSignedHttpClient();
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
    if (audioTrack == null) {
      audioTrack = PeerConnectionUtils.createAudioTrack(context, "mic");
      audioTrack.setEnabled(true);
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
    if (videoTrack == null) {
      videoTrack = PeerConnectionUtils.createVideoTrack(context, "cam");
      videoTrack.setEnabled(true);
    }
    workHandler.post(this::enableCameraImpl);
  }

  @WorkerThread
  private void enableMicImpl() {
    sendTransport.produce(
        producer -> {
          Logger.w(TAG, "onTransportClose()");
        },
        audioTrack,
        null,
        null);
  }

  @WorkerThread
  private void enableCameraImpl() {
    sendTransport.produce(
        producer -> {
          Logger.w(TAG, "onTransportClose()");
        },
        videoTrack,
        null,
        null);
  }

  @MainThread
  public void close() {
    if (protoo != null) {
      protoo.close();
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
        public void onFail() {}

        @Override
        public void onRequest(
            @NonNull Message.Request request, @NonNull Peer.ServerRequestHandler handler) {
          Logger.d(TAG, "onRequest() " + request.getData().toString());
        }

        @Override
        public void onNotification(@NonNull Message.Notification notification) {
          Logger.d(TAG, "onNotification() " + notification.getData().toString());
        }

        @Override
        public void onDisconnected() {}

        @Override
        public void onClose() {}
      };

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
              JSONObject device = new JSONObject();
              jsonPut(device, "flag", "android");
              jsonPut(device, "name", "Android " + Build.DEVICE);
              jsonPut(device, "version", Build.VERSION.CODENAME);

              JSONObject request = new JSONObject();
              jsonPut(request, "displayName", displayName);
              jsonPut(request, "device", device);
              jsonPut(request, "rtpCapabilities", toJsonObject(rtpCapabilities));
              // TODO(haiyangwu): add sctpCapabilities
              jsonPut(request, "sctpCapabilities", "");
              return protoo.request("join", request);
            })
        .doOnError(Throwable::printStackTrace)
        .subscribe(
            peers -> {
              Logger.d(TAG, "peers: " + peers);
              // TODO(haiyangwu): notify peers.
              workHandler.post(this::createSendTransport);
            });
  }

  @WorkerThread
  private void createSendTransport() {
    Logger.d(TAG, "createSendTransport()");
    JSONObject request = new JSONObject();
    jsonPut(request, "forceTcp", false);
    jsonPut(request, "producing", true);
    jsonPut(request, "consuming", false);
    jsonPut(request, "sctpCapabilities", "");

    protoo
        .request("createWebRtcTransport", request)
        .map(JSONObject::new)
        .doOnError(Throwable::printStackTrace)
        .subscribe(info -> workHandler.post(() -> createSendTransport(info)));
  }

  @WorkerThread
  private void createSendTransport(JSONObject transportInfo) {
    String id = transportInfo.optString("id");
    String iceParameters = transportInfo.optString("iceParameters");
    String iceCandidates = transportInfo.optString("iceCandidates");
    String dtlsParameters = transportInfo.optString("dtlsParameters");
    String sctpParameters = transportInfo.optString("sctpParameters");

    sendTransport =
        mediasoupDevice.createSendTransport(
            sendTransportListener, id, iceParameters, iceCandidates, dtlsParameters);
  }

  private SendTransport.Listener sendTransportListener =
      new SendTransport.Listener() {
        @Override
        public String onProduce(
            Transport transport, String kind, String rtpParameters, String appData) {
          Logger.d(TAG, "onProduce() ");

          JSONObject request = new JSONObject();
          jsonPut(request, "transportId", transport.getId());
          jsonPut(request, "kind", kind);
          jsonPut(request, "rtpParameters", toJsonObject(rtpParameters));
          jsonPut(request, "appData", appData);

          Logger.d(TAG, "send produce request with " + request.toString());
          String producerId = fetchProduceId(request);
          Logger.d(TAG, "producerId: " + producerId);
          return producerId;
        }

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
          Logger.d(TAG, "onConnect()");
          JSONObject request = new JSONObject();
          jsonPut(request, "transportId", transport.getId());
          jsonPut(request, "dtlsParameters", toJsonObject(dtlsParameters));
          protoo
              .request("connectWebRtcTransport", request)
              .subscribe(
                  data -> {
                    Logger.d(TAG, "connectWebRtcTransport res: " + data);
                  });
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
          Logger.d(TAG, "onConnectionStateChange: " + connectionState);
        }
      };

  private String fetchProduceId(JSONObject request) {
    // TODO: opt
    StringBuffer result = new StringBuffer();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    protoo
        .request("produce", request)
        .map(
            data -> {
              Logger.d(TAG, "produce result" + data);
              return toJsonObject(data).optString("id");
            })
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
}

package org.mediasoup.droid.lib;

import android.os.Build;
import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.Logger;
import org.protoojs.droid.Message;
import org.protoojs.droid.Peer;
import org.protoojs.droid.transports.WebSocketTransport;

import static org.mediasoup.droid.lib.Utils.jsonPut;

public class RoomClient {

  private static final String TAG = "RoomClient";

  private String roomId;
  private String peerId;
  private String displayName;
  private String device;

  private String protooUrl;
  private Peer protooPeer;

  private Device mediasoupDevice;

  public RoomClient(String roomId, String peerId, String displayName) {
    this(roomId, peerId, displayName, false, false);
  }

  public RoomClient(
      String roomId, String peerId, String displayName, boolean forceH264, boolean forceVP9) {
    this.roomId = roomId;
    this.peerId = peerId;
    this.displayName = displayName;
    this.protooUrl = UrlFactory.getProtooUrl(roomId, peerId, forceH264, forceVP9);
  }

  public void join() {
    Logger.d(TAG, "join() " + this.protooUrl);
    WebSocketTransport transport = new WebSocketTransport(protooUrl);
    protooPeer = new Peer(transport, peerListener);
  }

  public void close() {
    if (protooPeer != null) {
      protooPeer.close();
    }
    if (mediasoupDevice != null) {
      mediasoupDevice.dispose();
    }
  }

  private Peer.Listener peerListener =
      new Peer.Listener() {
        @Override
        public void onOpen() {
          joinImpl();
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

  private void joinImpl() {
    Logger.d(TAG, "joinImpl()");
    mediasoupDevice = new Device();
    protooPeer
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
              jsonPut(request, "rtpCapabilities", rtpCapabilities);
              // TODO(haiyangwu): add sctpCapabilities
              jsonPut(request, "sctpCapabilities", "");
              return protooPeer.request("join", request);
            })
        .subscribe(
            peers -> {
              Logger.d(TAG, "peers: " + peers);
            });
  }
}

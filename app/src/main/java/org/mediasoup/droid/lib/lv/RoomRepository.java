package org.mediasoup.droid.lib.lv;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.RemotePeers;
import org.mediasoup.droid.lib.model.RoomInfo;

public class RoomRepository {

  private SupplierMutableLiveData<RoomInfo> roomInfo = new SupplierMutableLiveData<>(RoomInfo::new);
  private MutableLiveData<Notify> notify = new MutableLiveData<>();
  private SupplierMutableLiveData<RemotePeers> peers =
      new SupplierMutableLiveData<>(RemotePeers::new);
  private SupplierMutableLiveData<Me> me = new SupplierMutableLiveData<>(Me::new);

  public SupplierMutableLiveData<RoomInfo> getRoomInfo() {
    return roomInfo;
  }

  public MutableLiveData<Notify> getNotify() {
    return notify;
  }

  public SupplierMutableLiveData<RemotePeers> getPeers() {
    return peers;
  }

  public SupplierMutableLiveData<Me> getMe() {
    return me;
  }

  public void setUrl(String url) {
    roomInfo.postValue(roomInfo -> roomInfo.setUrl(url));
  }

  public void setRoomState(RoomClient.RoomState state) {
    roomInfo.postValue(roomInfo -> roomInfo.setState(state));
  }

  public void notify(String text, long timeout) {
    notify.postValue(new Notify("info", text, timeout));
  }

  public void notify(String type, String text) {
    notify.postValue(new Notify(type, text));
  }

  public void addPeer(String id, JSONObject peerInfo) {
    peers.postValue(
        peersInfo -> peersInfo.getPeersMap().put(id, new RemotePeers.RemotePeer(id, peerInfo)));
  }

  public void setMediaCapabilities(boolean canSendMic, boolean canSendCam) {
    me.postValue(
        me -> {
          me.setCanSendMic(canSendMic);
          me.setCanSendCam(canSendCam);
        });
  }
}

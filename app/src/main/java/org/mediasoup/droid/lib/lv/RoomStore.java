package org.mediasoup.droid.lib.lv;

import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.DataConsumer;
import org.mediasoup.droid.DataProducer;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.model.Consumers;
import org.mediasoup.droid.lib.model.DataConsumers;
import org.mediasoup.droid.lib.model.DeviceInfo;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.Peers;
import org.mediasoup.droid.lib.model.Producers;
import org.mediasoup.droid.lib.model.RoomInfo;

/**
 * Room state.
 *
 * <p>Just like mediasoup-demo/app/lib/redux/stateActions.js
 */
@SuppressWarnings("unused")
public class RoomStore {

  private static final String TAG = "RoomStore";

  // room
  // mediasoup-demo/app/lib/redux/reducers/room.js
  private SupplierMutableLiveData<RoomInfo> roomInfo = new SupplierMutableLiveData<>(RoomInfo::new);

  // me
  // mediasoup-demo/app/lib/redux/reducers/me.js
  private SupplierMutableLiveData<Me> me = new SupplierMutableLiveData<>(Me::new);

  // producers
  // mediasoup-demo/app/lib/redux/reducers/producers.js
  private SupplierMutableLiveData<Producers> producers =
      new SupplierMutableLiveData<>(Producers::new);

  // peers
  // mediasoup-demo/app/lib/redux/reducers/peer.js
  private SupplierMutableLiveData<Peers> peers = new SupplierMutableLiveData<>(Peers::new);

  // consumers
  // mediasoup-demo/app/lib/redux/reducers/consumers.js
  private SupplierMutableLiveData<Consumers> consumers =
      new SupplierMutableLiveData<>(Consumers::new);

  // dataConsumers
  // mediasoup-demo/app/lib/redux/reducers/dataConsumers.js
  private SupplierMutableLiveData<DataConsumers> dataConsumers =
          new SupplierMutableLiveData<>(DataConsumers::new);

  // notify
  // mediasoup-demo/app/lib/redux/reducers/notifications.js
  private MutableLiveData<Notify> notify = new MutableLiveData<>();

  public void setRoomUrl(String roomId, String url) {
    roomInfo.postValue(
        roomInfo -> {
          roomInfo.setRoomId(roomId);
          roomInfo.setUrl(url);
        });
  }

  public void setRoomState(RoomClient.ConnectionState state) {
    roomInfo.postValue(roomInfo -> roomInfo.setConnectionState(state));

    if (RoomClient.ConnectionState.CLOSED.equals(state)) {
      peers.postValue(Peers::clear);
      me.postValue(Me::clear);
      producers.postValue(Producers::clear);
      consumers.postValue(Consumers::clear);
    }
  }

  public void setRoomActiveSpeaker(String peerId) {
    roomInfo.postValue(roomInfo -> roomInfo.setActiveSpeakerId(peerId));
  }

  public void setRoomStatsPeerId(String peerId) {
    roomInfo.postValue(roomInfo -> roomInfo.setStatsPeerId(peerId));
  }

  public void setRoomFaceDetection(boolean enable) {
    roomInfo.postValue(roomInfo -> roomInfo.setFaceDetection(enable));
  }

  public void setMe(String peerId, String displayName, DeviceInfo device) {
    me.postValue(
        me -> {
          me.setId(peerId);
          me.setDisplayName(displayName);
          me.setDevice(device);
        });
  }

  public void setMediaCapabilities(boolean canSendMic, boolean canSendCam) {
    me.postValue(
        me -> {
          me.setCanSendMic(canSendMic);
          me.setCanSendCam(canSendCam);
        });
  }

  public void setCanChangeCam(boolean canChangeCam) {
    me.postValue(me -> me.setCanSendCam(canChangeCam));
  }

  public void setDisplayName(String displayName) {
    me.postValue(me -> me.setDisplayName(displayName));
  }

  public void setAudioOnlyState(boolean enabled) {
    me.postValue(me -> me.setAudioOnly(enabled));
  }

  public void setAudioOnlyInProgress(boolean enabled) {
    me.postValue(me -> me.setAudioOnlyInProgress(enabled));
  }

  public void setAudioMutedState(boolean enabled) {
    me.postValue(me -> me.setAudioMuted(enabled));
  }

  public void setRestartIceInProgress(boolean restartIceInProgress) {
    me.postValue(me -> me.setRestartIceInProgress(restartIceInProgress));
  }

  public void setCamInProgress(boolean inProgress) {
    me.postValue(me -> me.setCamInProgress(inProgress));
  }

  public void addProducer(Producer producer) {
    producers.postValue(producers -> producers.addProducer(producer));
  }

  public void setProducerPaused(String producerId) {
    producers.postValue(producers -> producers.setProducerPaused(producerId));
  }

  public void setProducerResumed(String producerId) {
    producers.postValue(producers -> producers.setProducerResumed(producerId));
  }

  public void removeProducer(String producerId) {
    producers.postValue(producers -> producers.removeProducer(producerId));
  }

  public void setProducerScore(String producerId, JSONArray score) {
    producers.postValue(producers -> producers.setProducerScore(producerId, score));
  }

  public void addDataProducer(DataProducer dataProducer) {
  }

  public void removeDataProducer(String dataProducerId) {
  }

  public void addPeer(String peerId, JSONObject peerInfo) {
    peers.postValue(peersInfo -> peersInfo.addPeer(peerId, peerInfo));
  }

  public void setPeerDisplayName(String peerId, String displayName) {
    peers.postValue(peersInfo -> peersInfo.setPeerDisplayName(peerId, displayName));
  }

  public void removePeer(String peerId) {
    roomInfo.postValue(
        roomInfo -> {
          if (!TextUtils.isEmpty(peerId) && peerId.equals(roomInfo.getActiveSpeakerId())) {
            roomInfo.setActiveSpeakerId(null);
          }
          if (!TextUtils.isEmpty(peerId) && peerId.equals(roomInfo.getStatsPeerId())) {
            roomInfo.setStatsPeerId(null);
          }
        });
    peers.postValue(peersInfo -> peersInfo.removePeer(peerId));
  }

  public void addConsumer(String peerId, String type, Consumer consumer, boolean remotelyPaused) {
    consumers.postValue(consumers -> consumers.addConsumer(type, consumer, remotelyPaused));
    peers.postValue(peers -> peers.addConsumer(peerId, consumer));
  }

  public void removeConsumer(String peerId, String consumerId) {
    consumers.postValue(consumers -> consumers.removeConsumer(consumerId));
    peers.postValue(peers -> peers.removeConsumer(peerId, consumerId));
  }

  public void setConsumerPaused(String consumerId, String originator) {
    consumers.postValue(consumers -> consumers.setConsumerPaused(consumerId, originator));
  }

  public void setConsumerResumed(String consumerId, String originator) {
    consumers.postValue(consumers -> consumers.setConsumerResumed(consumerId, originator));
  }

  public void setConsumerCurrentLayers(String consumerId, int spatialLayer, int temporalLayer) {
    consumers.postValue(
        consumers -> consumers.setConsumerCurrentLayers(consumerId, spatialLayer, temporalLayer));
  }

  public void setConsumerScore(String consumerId, JSONArray score) {
    consumers.postValue(consumers -> consumers.setConsumerScore(consumerId, score));
  }

  public void addDataConsumer(String peerId, DataConsumer dataConsumer) {
    dataConsumers.postValue(dataConsumers -> dataConsumers.addDataConsumer(dataConsumer));
    peers.postValue(peers -> peers.addDataConsumer(peerId, dataConsumer));
  }

  public void removeDataConsumer(String peerId, String dataConsumerId) {
    dataConsumers.postValue(dataConsumers -> dataConsumers.removeDataConsumer(dataConsumerId));
    peers.postValue(peers -> peers.removeDataConsumer(peerId, dataConsumerId));
  }

  public void addNotify(String text) {
    notify.postValue(new Notify("info", text));
  }

  public void addNotify(String text, int timeout) {
    notify.postValue(new Notify("info", text, timeout));
  }

  public void addNotify(String type, String text) {
    notify.postValue(new Notify(type, text));
  }

  public void addNotifyMessage(String title, String text) {
    notify.postValue(new Notify("message", title, text, 5000));
  }

  public void addNotify(String text, Throwable throwable) {
    notify.postValue(new Notify("error", text + throwable.getMessage()));
  }

  public SupplierMutableLiveData<RoomInfo> getRoomInfo() {
    return roomInfo;
  }

  public SupplierMutableLiveData<Me> getMe() {
    return me;
  }

  public MutableLiveData<Notify> getNotify() {
    return notify;
  }

  public SupplierMutableLiveData<Peers> getPeers() {
    return peers;
  }

  public SupplierMutableLiveData<Producers> getProducers() {
    return producers;
  }

  public SupplierMutableLiveData<Consumers> getConsumers() {
    return consumers;
  }
}

package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.DataConsumer;
import org.mediasoup.droid.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Peers {

  private static final String TAG = "Peers";

  private Map<String, Peer> mPeersInfo;

  public Peers() {
    mPeersInfo = Collections.synchronizedMap(new LinkedHashMap<>());
  }

  public void addPeer(String peerId, @NonNull JSONObject peerInfo) {
    mPeersInfo.put(peerId, new Peer(peerInfo));
  }

  public void removePeer(String peerId) {
    mPeersInfo.remove(peerId);
  }

  public void setPeerDisplayName(String peerId, String displayName) {
    Peer peer = mPeersInfo.get(peerId);
    if (peer == null) {
      Logger.e(TAG, "no Protoo found");
      return;
    }
    peer.setDisplayName(displayName);
  }

  public void addConsumer(String peerId, Consumer consumer) {
    Peer peer = getPeer(peerId);
    if (peer == null) {
      Logger.e(TAG, "no Peer found for new Consumer");
      return;
    }

    peer.getConsumers().add(consumer.getId());
  }

  public void removeConsumer(String peerId, String consumerId) {
    Peer peer = getPeer(peerId);
    if (peer == null) {
      return;
    }

    peer.getConsumers().remove(consumerId);
  }

  public void addDataConsumer(String peerId, DataConsumer consumer) {
    Peer peer = getPeer(peerId);
    if (peer == null) {
      Logger.e(TAG, "no Peer found for new Data Consumer");
      return;
    }

    peer.getDataConsumers().add(consumer.getId());
  }

  public void removeDataConsumer(String peerId, String consumerId) {
    Peer peer = getPeer(peerId);
    if (peer == null) {
      return;
    }

    peer.getDataConsumers().remove(consumerId);
  }

  public Peer getPeer(String peerId) {
    return mPeersInfo.get(peerId);
  }

  public List<Peer> getAllPeers() {
    List<Peer> peers = new ArrayList<>();
    for (Map.Entry<String, Peer> info : mPeersInfo.entrySet()) {
      peers.add(info.getValue());
    }
    return peers;
  }

  public void clear() {
    mPeersInfo.clear();
  }
}

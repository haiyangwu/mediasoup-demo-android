package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.JsonUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RemotePeers {

  private static final String TAG = "RemotePeers";

  public static class RemotePeer {
    private String id;
    private @NonNull JSONObject info;
    private @NonNull Set<String> consumers;
    private @NonNull Set<String> dataConsumers;

    public RemotePeer(String id, JSONObject info) {
      this.id = id;
      this.info = info == null ? new JSONObject() : info;
      this.consumers = new HashSet<>();
      this.dataConsumers = new HashSet<>();
    }

    public String getId() {
      return id;
    }

    @NonNull
    public JSONObject getInfo() {
      return info;
    }

    @NonNull
    public Set<String> getConsumers() {
      return consumers;
    }

    @NonNull
    public Set<String> getDataConsumers() {
      return dataConsumers;
    }
  }

  private Map<String, RemotePeer> peersMap = new HashMap<>();

  public void addPeer(String peerId, JSONObject peerInfo) {
    peersMap.put(peerId, new RemotePeers.RemotePeer(peerId, peerInfo));
  }

  public void setPeerDisplayName(String peerId, String displayName) {
    RemotePeer peer = peersMap.get(peerId);
    if (peer == null) {
      Logger.e(TAG, "no Peer found");
      return;
    }
    JsonUtils.jsonPut(peer.getInfo(), "displayName", displayName);
  }

  public void addConsumer(String peerId, Consumer consumer) {
    RemotePeer peer = peersMap.get(peerId);
    if (peer == null) {
      Logger.e(TAG, "no Peer found for new Consumer");
      return;
    }
    peer.getConsumers().add(consumer.getId());
  }

  public void removeConsumer(String peerId, String consumerId) {
    RemotePeer peer = peersMap.get(peerId);
    if (peer == null) {
      Logger.e(TAG, "Consumer not found");
      return;
    }
    peer.getConsumers().remove(consumerId);
  }

  public void removePeer(String peerId) {
    peersMap.remove(peerId);
  }

  public void clear() {
    peersMap.clear();
  }
}

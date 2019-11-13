package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.mediasoup.droid.Consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemotePeers {

  public static class RemotePeer {
    private String id;
    private JSONObject info;
    private List<Consumer> consumers;

    public RemotePeer(String id, JSONObject info) {
      this.id = id;
      this.info = info;
      this.consumers = new ArrayList<>();
    }

    public String getId() {
      return id;
    }

    public JSONObject getInfo() {
      return info;
    }

    public List<Consumer> getConsumers() {
      return consumers;
    }
  }

  private Map<String, RemotePeer> peersMap = new HashMap<>();

  @NonNull
  public Map<String, RemotePeer> getPeersMap() {
    return peersMap;
  }
}

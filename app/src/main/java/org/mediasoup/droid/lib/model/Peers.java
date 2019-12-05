package org.mediasoup.droid.lib.model;

import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.JsonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class Peers {

  private static final String TAG = "Peers";

  private Map<String, JSONObject> peersInfo = new LinkedHashMap<>();

  public void addPeer(String peerId, JSONObject peerInfo) {
    peersInfo.put(peerId, peerInfo);
  }

  public void setPeerDisplayName(String peerId, String displayName) {
    JSONObject info = peersInfo.get(peerId);
    if (info == null) {
      Logger.e(TAG, "no Protoo found");
      return;
    }
    JsonUtils.jsonPut(info, "displayName", displayName);
  }

  public Map<String, JSONObject> getPeersInfo() {
    return peersInfo;
  }

  public void removePeer(String peerId) {
    peersInfo.remove(peerId);
  }

  public void clear() {
    peersInfo.clear();
  }
}

package org.mediasoup.droid.lib;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtils {

  public static void jsonPut(JSONObject json, String key, Object value) {
    try {
      json.put(key, value);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public static JSONObject toJsonObject(String data) {
    try {
      return new JSONObject(data);
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
  }
}

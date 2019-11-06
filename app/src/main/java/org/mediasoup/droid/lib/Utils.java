package org.mediasoup.droid.lib;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class Utils {

  private static final String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

  public static String getRandomString(final int sizeOfRandomString) {
    final Random random = new Random();
    final StringBuilder sb = new StringBuilder(sizeOfRandomString);
    for (int i = 0; i < sizeOfRandomString; ++i)
      sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
    return sb.toString();
  }

  public static void jsonPut(JSONObject json, String key, Object value) {
    try {
      json.put(key, value);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
}

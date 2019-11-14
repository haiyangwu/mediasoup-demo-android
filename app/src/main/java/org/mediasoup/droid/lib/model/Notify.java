package org.mediasoup.droid.lib.model;

import org.mediasoup.droid.lib.Utils;

public class Notify {

  private String id;
  private String type;
  private String text;
  private long timeout;

  public Notify(String type, String text) {
    this(type, text, 0);
  }

  public Notify(String type, String text, long timeout) {
    this.id = Utils.getRandomString(6).toLowerCase();
    this.type = type;
    this.text = text;
    this.timeout = timeout;
    if (this.timeout == 0) {
      if ("info".equals(this.type)) {
        this.timeout = 3000;
      } else if ("error".equals(this.type)) {
        this.timeout = 5000;
      }
    }
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getText() {
    return text;
  }

  public long getTimeout() {
    return timeout;
  }
}

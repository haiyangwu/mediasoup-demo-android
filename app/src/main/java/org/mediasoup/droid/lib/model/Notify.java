package org.mediasoup.droid.lib.model;

import org.mediasoup.droid.lib.Utils;

public class Notify {

  private String mId;
  private String mType;
  private String mText;
  private int mTimeout;

  public Notify(String type, String text) {
    this(type, text, 0);
  }

  public Notify(String type, String text, int timeout) {
    this.mId = Utils.getRandomString(6).toLowerCase();
    this.mType = type;
    this.mText = text;
    this.mTimeout = timeout;
    if (this.mTimeout == 0) {
      if ("info".equals(this.mType)) {
        this.mTimeout = 3000;
      } else if ("error".equals(this.mType)) {
        this.mTimeout = 5000;
      }
    }
  }

  public String getId() {
    return mId;
  }

  public String getType() {
    return mType;
  }

  public String getText() {
    return mText;
  }

  public int getTimeout() {
    return mTimeout;
  }
}

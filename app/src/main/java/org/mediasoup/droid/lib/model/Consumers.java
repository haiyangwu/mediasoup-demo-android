package org.mediasoup.droid.lib.model;

import org.json.JSONArray;
import org.mediasoup.droid.Consumer;

import java.util.HashMap;
import java.util.Map;

public class Consumers {

  public static class ConsumerWrapper {

    private boolean mLocallyPaused;
    private boolean mRemotelyPaused;
    private Consumer mConsumer;
    private JSONArray mScore;

    ConsumerWrapper(Consumer consumer) {
      this.mConsumer = consumer;
    }

    public Consumer getConsumer() {
      return mConsumer;
    }

    public boolean isLocallyPaused() {
      return mLocallyPaused;
    }

    public boolean isRemotelyPaused() {
      return mRemotelyPaused;
    }

    public JSONArray getScore() {
      return mScore;
    }
  }

  private final Map<String, ConsumerWrapper> consumers;

  public Consumers() {
    consumers = new HashMap<>();
  }

  public void addConsumer(Consumer consumer) {
    consumers.put(consumer.getId(), new ConsumerWrapper(consumer));
  }

  public void removeConsumer(String consumerId) {
    consumers.remove(consumerId);
  }

  public void setConsumerPaused(String consumerId, String originator) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }

    if ("local".equals(originator)) {
      wrapper.mLocallyPaused = true;
    } else {
      wrapper.mRemotelyPaused = true;
    }
  }

  public void setConsumerResumed(String consumerId, String originator) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }

    if ("local".equals(originator)) {
      wrapper.mLocallyPaused = false;
    } else {
      wrapper.mRemotelyPaused = false;
    }
  }

  public void setConsumerScore(String consumerId, JSONArray score) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }

    wrapper.mScore = score;
  }

  public ConsumerWrapper getConsumer(String consumerId) {
    return consumers.get(consumerId);
  }

  public void clear() {
    consumers.clear();
  }
}

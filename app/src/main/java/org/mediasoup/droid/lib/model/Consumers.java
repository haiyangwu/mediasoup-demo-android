package org.mediasoup.droid.lib.model;

import org.mediasoup.droid.Consumer;

import java.util.HashMap;
import java.util.Map;

public class Consumers {

  public static class ConsumerWrapper {
    private Consumer consumer;
    private boolean locallyPaused;
    private boolean remotelyPaused;
    private int score;

    ConsumerWrapper(Consumer consumer) {
      this.consumer = consumer;
    }

    public Consumer getConsumer() {
      return consumer;
    }

    public boolean isLocallyPaused() {
      return locallyPaused;
    }

    public boolean isRemotelyPaused() {
      return remotelyPaused;
    }

    public int getScore() {
      return score;
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
      wrapper.locallyPaused = true;
    } else {
      wrapper.remotelyPaused = true;
    }
  }

  public void setConsumerResumed(String consumerId, String originator) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }

    if ("local".equals(originator)) {
      wrapper.locallyPaused = false;
    } else {
      wrapper.remotelyPaused = false;
    }
  }

  public void setConsumerScore(String consumerId, int score) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }

    wrapper.score = score;
  }

  public void clear() {
    consumers.clear();
  }
}

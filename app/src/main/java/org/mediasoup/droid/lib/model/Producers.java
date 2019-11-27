package org.mediasoup.droid.lib.model;

import org.mediasoup.droid.Producer;

import java.util.HashMap;
import java.util.Map;

public class Producers {

  public static class ProducersWrapper {
    private Producer producer;
    private int score;

    ProducersWrapper(Producer producer) {
      this.producer = producer;
    }

    public Producer getProducer() {
      return producer;
    }

    public int getScore() {
      return score;
    }
  }

  private final Map<String, ProducersWrapper> producers;

  public Producers() {
    producers = new HashMap<>();
  }

  public void addProducer(Producer producer) {
    producers.put(producer.getId(), new ProducersWrapper(producer));
  }

  public void removeProducer(String producerId) {
    producers.remove(producerId);
  }

  public void setProducerPaused(String producerId) {
    ProducersWrapper wrapper = producers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.producer.pause();
  }

  public void setProducerResumed(String producerId) {
    ProducersWrapper wrapper = producers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.producer.resume();
  }

  public void setProducerScore(String producerId, int score) {
    ProducersWrapper wrapper = producers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.score = score;
  }

  public void clear() {
    producers.clear();
  }
}

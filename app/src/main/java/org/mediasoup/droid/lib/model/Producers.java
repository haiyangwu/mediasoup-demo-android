package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.mediasoup.droid.Producer;

import java.util.HashMap;
import java.util.Map;

public class Producers {

  public static class ProducersWrapper {
    private Producer producer;
    private JSONArray score;

    ProducersWrapper(Producer producer) {
      this.producer = producer;
    }

    public Producer getProducer() {
      return producer;
    }

    public JSONArray getScore() {
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

  public void setProducerScore(String producerId, JSONArray score) {
    ProducersWrapper wrapper = producers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.score = score;
  }

  public Producer filter(@NonNull String kind) {
    for (ProducersWrapper wrapper : producers.values()) {
      if (wrapper.producer == null) {
        continue;
      }
      if (wrapper.producer.getTrack() == null) {
        continue;
      }
      if (kind.equals(wrapper.producer.getTrack().kind())) {
        return wrapper.producer;
      }
    }

    return null;
  }

  public void clear() {
    producers.clear();
  }
}

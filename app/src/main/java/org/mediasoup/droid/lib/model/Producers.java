package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.mediasoup.droid.Producer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Producers {

  public static class ProducersWrapper {

    public static final String TYPE_CAM = "cam";
    public static final String TYPE_SHARE = "share";

    private Producer mProducer;
    private JSONArray mScore;
    private String mType;

    ProducersWrapper(Producer producer) {
      this.mProducer = producer;
    }

    public Producer getProducer() {
      return mProducer;
    }

    public JSONArray getScore() {
      return mScore;
    }

    public String getType() {
      return mType;
    }

    public void setType(String type) {
      mType = type;
    }
  }

  private final Map<String, ProducersWrapper> mProducers;

  public Producers() {
    mProducers = new ConcurrentHashMap<>();
  }

  public void addProducer(Producer producer) {
    mProducers.put(producer.getId(), new ProducersWrapper(producer));
  }

  public void removeProducer(String producerId) {
    mProducers.remove(producerId);
  }

  public void setProducerPaused(String producerId) {
    ProducersWrapper wrapper = mProducers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.mProducer.pause();
  }

  public void setProducerResumed(String producerId) {
    ProducersWrapper wrapper = mProducers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.mProducer.resume();
  }

  public void setProducerScore(String producerId, JSONArray score) {
    ProducersWrapper wrapper = mProducers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.mScore = score;
  }

  public ProducersWrapper filter(@NonNull String kind) {
    for (ProducersWrapper wrapper : mProducers.values()) {
      if (wrapper.mProducer == null) {
        continue;
      }
      if (wrapper.mProducer.getTrack() == null) {
        continue;
      }
      if (kind.equals(wrapper.mProducer.getTrack().kind())) {
        return wrapper;
      }
    }

    return null;
  }

  public void clear() {
    mProducers.clear();
  }
}

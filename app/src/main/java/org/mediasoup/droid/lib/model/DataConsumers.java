package org.mediasoup.droid.lib.model;

import org.mediasoup.droid.DataConsumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataConsumers {

  private final Map<String, DataConsumer> dataConsumers;

  public DataConsumers() {
    dataConsumers = new ConcurrentHashMap<>();
  }

  public void addDataConsumer(DataConsumer dataConsumer) {
    dataConsumers.put(dataConsumer.getId(), dataConsumer);
  }

  public void removeDataConsumer(String dataConsumerId) {
    dataConsumers.remove(dataConsumerId);
  }
}

package org.mediasoup.droid.lib;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.DataConsumer;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.protoojs.droid.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RoomMessageHandler {

  static final String TAG = "RoomClient";

  // Stored Room States.
  @NonNull final RoomStore mStore;
  // mediasoup Consumers.
  @NonNull final Map<String, ConsumerHolder> mConsumers;
  @NonNull final Map<String, DataConsumerHolder> mDataConsumers;

  static class ConsumerHolder {
    @NonNull final String peerId;
    @NonNull final Consumer mConsumer;

    ConsumerHolder(@NonNull String peerId, @NonNull Consumer consumer) {
      this.peerId = peerId;
      mConsumer = consumer;
    }
  }

  static class DataConsumerHolder {
    @NonNull final String peerId;
    @NonNull final DataConsumer mDataConsumer;

    DataConsumerHolder(@NonNull String peerId, @NonNull DataConsumer dataConsumer) {
      this.peerId = peerId;
      mDataConsumer = dataConsumer;
    }
  }

  RoomMessageHandler(@NonNull RoomStore store) {
    this.mStore = store;
    this.mConsumers = new ConcurrentHashMap<>();
    this.mDataConsumers = new ConcurrentHashMap<>();
  }

  @WorkerThread
  void handleNotification(Message.Notification notification) throws JSONException {
    JSONObject data = notification.getData();
    switch (notification.getMethod()) {
      case "producerScore":
        {
          // {"producerId":"bdc2e83e-5294-451e-a986-a29c7d591d73","score":[{"score":10,"ssrc":196184265}]}
          String producerId = data.getString("producerId");
          JSONArray score = data.getJSONArray("score");
          mStore.setProducerScore(producerId, score);
          break;
        }
      case "newPeer":
        {
          String id = data.getString("id");
          String displayName = data.optString("displayName");
          mStore.addPeer(id, data);
          mStore.addNotify(displayName + " has joined the room");
          break;
        }
      case "peerClosed":
        {
          String peerId = data.getString("peerId");
          mStore.removePeer(peerId);
          break;
        }
      case "peerDisplayNameChanged":
        {
          String peerId = data.getString("peerId");
          String displayName = data.optString("displayName");
          String oldDisplayName = data.optString("oldDisplayName");
          mStore.setPeerDisplayName(peerId, displayName);
          mStore.addNotify(oldDisplayName + " is now " + displayName);
          break;
        }
      case "consumerClosed":
        {
          String consumerId = data.getString("consumerId");
          ConsumerHolder holder = mConsumers.remove(consumerId);
          if (holder == null) {
            break;
          }
          holder.mConsumer.close();
          mConsumers.remove(consumerId);
          mStore.removeConsumer(holder.peerId, holder.mConsumer.getId());
          break;
        }
      case "consumerPaused":
        {
          String consumerId = data.getString("consumerId");
          ConsumerHolder holder = mConsumers.get(consumerId);
          if (holder == null) {
            break;
          }
          mStore.setConsumerPaused(holder.mConsumer.getId(), "remote");
          break;
        }
      case "consumerResumed":
        {
          String consumerId = data.getString("consumerId");
          ConsumerHolder holder = mConsumers.get(consumerId);
          if (holder == null) {
            break;
          }
          mStore.setConsumerResumed(holder.mConsumer.getId(), "remote");
          break;
        }
      case "consumerLayersChanged":
        {
          String consumerId = data.getString("consumerId");
          int spatialLayer = data.optInt("spatialLayer");
          int temporalLayer = data.optInt("temporalLayer");
          ConsumerHolder holder = mConsumers.get(consumerId);
          if (holder == null) {
            break;
          }
          mStore.setConsumerCurrentLayers(consumerId, spatialLayer, temporalLayer);
          break;
        }
      case "consumerScore":
        {
          String consumerId = data.getString("consumerId");
          JSONArray score = data.optJSONArray("score");
          ConsumerHolder holder = mConsumers.get(consumerId);
          if (holder == null) {
            break;
          }
          mStore.setConsumerScore(consumerId, score);
          break;
        }
      case "dataConsumerClosed":
        {
          String dataConsumerId = data.getString("dataConsumerId");
          DataConsumerHolder dataConsumer = mDataConsumers.get(dataConsumerId);
          if (dataConsumer == null) {
            break;
          }

          dataConsumer.mDataConsumer.close();
          mStore.removeDataConsumer(dataConsumer.peerId, dataConsumer.mDataConsumer.getId());
          break;
        }
      case "activeSpeaker":
        {
          String peerId = data.getString("peerId");
          mStore.setRoomActiveSpeaker(peerId);
          break;
        }
      default:
        {
          Logger.e(TAG, "unknown protoo notification.method " + notification.getMethod());
        }
    }
  }
}

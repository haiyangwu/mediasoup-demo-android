package org.mediasoup.droid.lib;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.protoojs.droid.Message;

class RoomMessageHandler {

  static final String TAG = "RoomClient";

  // Stored Room States.
  @NonNull final RoomStore mStore;

  RoomMessageHandler(@NonNull RoomStore store) {
    this.mStore = store;
  }

  @WorkerThread
  void handleRequest(Message.Request request, Protoo.ServerRequestHandler handler) {
    // TODO (HaiyangWu): handle request msg
    switch (request.getMethod()) {
      case "newConsumer":
        {
          break;
        }
      case "newDataConsumer":
        {
          break;
        }
    }
  }

  @WorkerThread
  void handleNotification(Message.Notification notification) throws JSONException {
    // TODO (HaiyangWu): handle notification msg
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
          break;
        }
      case "consumerPaused":
        {
          break;
        }
      case "consumerResumed":
        {
          break;
        }
      case "consumerLayersChanged":
        {
          break;
        }
      case "consumerScore":
        {
          break;
        }
      case "dataConsumerClosed":
        {
          break;
        }
      case "activeSpeaker":
        {
          break;
        }
      default:
        {
          Logger.e(TAG, "unknown protoo notification.method " + notification.getMethod());
        }
    }
  }
}

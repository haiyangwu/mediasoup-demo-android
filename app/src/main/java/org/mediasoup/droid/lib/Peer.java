package org.mediasoup.droid.lib;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.protoojs.droid.ProtooException;
import org.mediasoup.droid.lib.socket.WebSocketTransport;

import io.reactivex.rxjava3.core.Observable;

public class Peer extends org.protoojs.droid.Peer {

  public Peer(@NonNull WebSocketTransport transport, @NonNull Listener listener) {
    super(transport, listener);
  }

  public Observable<String> request(String method) {
    return request(method, new JSONObject());
  }

  public Observable<String> request(String method, @NonNull JSONObject data) {
    return Observable.create(
        emitter ->
            request(
                method,
                data,
                new ClientRequestHandler() {
                  @Override
                  public void resolve(String data) {
                    if (!emitter.isDisposed()) {
                      emitter.onNext(data);
                    }
                  }

                  @Override
                  public void reject(long error, String errorReason) {
                    if (!emitter.isDisposed()) {
                      emitter.onError(new ProtooException(error, errorReason));
                    }
                  }
                }));
  }
}

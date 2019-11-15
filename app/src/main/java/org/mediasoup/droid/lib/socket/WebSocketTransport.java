package org.mediasoup.droid.lib.socket;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.protoojs.droid.Message;
import org.protoojs.droid.transports.AbsWebSocketTransport;

import io.crossbar.autobahn.websocket.WebSocketConnection;
import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.interfaces.IWebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.types.ConnectionResponse;

public class WebSocketTransport extends AbsWebSocketTransport
    implements IWebSocketConnectionHandler {

  // Log tag.
  private static final String TAG = "WebSocketTransport";
  // Closed flag.
  private boolean mClosed;
  // Retry operation.
  private RetryStrategy mRetryStrategy;
  // WebSocketConnection instance.
  private WebSocketConnection mWebSocketConnection;
  // Listener.
  private Listener mListener;

  private Handler mHandler = new Handler(Looper.getMainLooper());

  private static class RetryStrategy {

    private final int retries;
    private final int factor;
    private final int minTimeout;
    private final int maxTimeout;

    private int retryCount = 0;

    RetryStrategy(int retries, int factor, int minTimeout, int maxTimeout) {
      this.retries = retries;
      this.factor = factor;
      this.minTimeout = minTimeout;
      this.maxTimeout = maxTimeout;
    }

    void retried() {
      retryCount++;
    }

    int getReconnectInterval() {
      Logger.d(TAG, "getReconnectInterval() ");
      if (retryCount > retries) {
        return -1;
      }
      int reconnectInterval = (int) (minTimeout * Math.pow(factor, retryCount));
      reconnectInterval = Math.min(reconnectInterval, maxTimeout);
      return reconnectInterval;
    }

    void reset() {
      if (retryCount != 0) {
        retryCount = 0;
      }
    }
  }

  public WebSocketTransport(String url) {
    super(url);
    mRetryStrategy = new RetryStrategy(10, 2, 1000, 8 * 1000);
    mWebSocketConnection = new WebSocketConnection();
  }

  @Override
  public void connect(Listener listener) {
    mListener = listener;
    try {
      String[] wsSubprotocols = {"protoo"};
      mWebSocketConnection.connect(mUrl, wsSubprotocols, this, null, null);
    } catch (WebSocketException ex) {
      Logger.e(TAG, "", ex);
    }
  }

  private boolean scheduleReconnect() {
    Logger.d(TAG, "scheduleReconnect()");
    int reconnectInterval = mRetryStrategy.getReconnectInterval();
    if (reconnectInterval == -1) {
      return false;
    }
    mHandler.postDelayed(
        () -> {
          if (mClosed) {
            return;
          }
          if (mWebSocketConnection != null) {
            Logger.d(TAG, "doing reconnect job");
            mWebSocketConnection.reconnect();
            mRetryStrategy.retried();
          }
        },
        reconnectInterval);
    return true;
  }

  @Override
  public String sendMessage(JSONObject message) {
    if (mClosed) {
      throw new IllegalStateException("transport closed");
    }
    String payload = message.toString();
    mWebSocketConnection.sendMessage(payload);
    return payload;
  }

  @Override
  public void close() {
    if (mClosed) {
      return;
    }
    Logger.d(TAG, "close()");
    mWebSocketConnection.sendClose();
  }

  @Override
  public boolean isClosed() {
    return mClosed;
  }

  @Override
  public void onConnect(ConnectionResponse response) {
    Logger.d(TAG, "onConnect()");
  }

  @Override
  public void onOpen() {
    if (mClosed) {
      return;
    }
    Logger.d(TAG, "onOpen()");
    if (mListener != null) {
      mListener.onOpen();
    }
    mRetryStrategy.reset();
  }

  @Override
  public void onClose(int code, String reason) {
    if (mClosed) {
      return;
    }

    if (code == IWebSocketConnectionHandler.CLOSE_RECONNECT) {
      throw new IllegalStateException("reconnect should out of WebSocketConnection");
    }

    Logger.w(TAG, "onClose() code: " + code + ", reason: " + reason);

    boolean shouldReconnect =
        (code == IWebSocketConnectionHandler.CLOSE_CANNOT_CONNECT)
            || (code == IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST);

    if (shouldReconnect && scheduleReconnect()) {
      if (mListener != null) {
        if (code == IWebSocketConnectionHandler.CLOSE_CANNOT_CONNECT) {
          mListener.onFail();
        } else {
          mListener.onDisconnected();
        }
      }
      return;
    }

    mClosed = true;
    if (mListener != null) {
      mListener.onClose();
    }
    mRetryStrategy.reset();
  }

  @Override
  public void onMessage(String payload) {
    Logger.d(TAG, "onMessage()");
    Message message = Message.parse(payload);
    if (message == null) {
      return;
    }
    if (mListener != null) {
      mListener.onMessage(message);
    }
  }

  @Override
  public void onMessage(byte[] payload, boolean isBinary) {
    Logger.d(TAG, "onMessage()");
  }

  @Override
  public void onPing() {
    Logger.d(TAG, "onPing()");
  }

  @Override
  public void onPing(byte[] payload) {
    Logger.d(TAG, "onPing()");
  }

  @Override
  public void onPong() {
    Logger.d(TAG, "onPong()");
  }

  @Override
  public void onPong(byte[] payload) {
    Logger.d(TAG, "onPong()");
  }

  @Override
  public void setConnection(WebSocketConnection connection) {
    Logger.d(TAG, "setConnection()");
  }
}

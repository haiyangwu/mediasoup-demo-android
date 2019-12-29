package org.mediasoup.droid.lib;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class UrlFactory {

  private static final String HOSTNAME = "v3demo.mediasoup.org";
  //  private static final String HOSTNAME = "192.168.1.103";
  private static final int PORT = 4443;

  public static String getInvitationLink(String roomId, boolean forceH264, boolean forceVP9) {
    String url = String.format(Locale.US, "https://%s/?roomId=%s", HOSTNAME, roomId);
    if (forceH264) {
      url += "&forceH264=true";
    } else if (forceVP9) {
      url += "&forceVP9=true";
    }
    return url;
  }

  public static String getProtooUrl(
      String roomId, String peerId, boolean forceH264, boolean forceVP9) {
    String url =
        String.format(
            Locale.US, "wss://%s:%d/?roomId=%s&peerId=%s", HOSTNAME, PORT, roomId, peerId);
    if (forceH264) {
      url += "&forceH264=true";
    } else if (forceVP9) {
      url += "&forceVP9=true";
    }
    return url;
  }

  public static void enableSelfSignedHttpClient() {
    final TrustManager[] trustManagers =
        new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[] {};
            }
          }
        };

    try {
      final SSLContext sslContext;
      sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustManagers, new SecureRandom());
      SSLContext.setDefault(sslContext);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }
  }
}

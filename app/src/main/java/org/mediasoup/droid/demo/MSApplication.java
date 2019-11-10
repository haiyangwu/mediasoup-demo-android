package org.mediasoup.droid.demo;

import android.app.Application;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupClient;

public class MSApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.setLogLevel(Logger.LogLevel.LOG_TRACE);
        Logger.setDefaultHandler();
        MediasoupClient.initialize(getApplicationContext());
    }
}

package com.postech.isb.util;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by torrent on 2016-12-14.
 */
public class TimeService extends Service {
    // constant
    public static final long HEARTBEAT_INTERVAL = 1 * 1000; // 10 seconds

    // run on another Thread to avoid crash
    private Handler mHandler = new Handler();
    // timer handling
    private Timer mTimer = null;
    private Messenger heartbeatMessageHandler;

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        heartbeatMessageHandler = (Messenger) extras.get("MESSENGER");
        return super.onStartCommand (intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        // cancel if already existed
        if (mTimer != null) {
            mTimer.cancel();
        } else {
            // recreate new
            mTimer = new Timer();
        }
        // schedule task
        mTimer.scheduleAtFixedRate(new HeartbeatTimerTask(), 0, HEARTBEAT_INTERVAL);
    }

    class HeartbeatTimerTask extends TimerTask {
        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    try {
                        heartbeatMessageHandler.send(new Message());
                    }
                    catch (RemoteException e) {
                        // Do nothing
                    }
                }
            });
        }
    }
}
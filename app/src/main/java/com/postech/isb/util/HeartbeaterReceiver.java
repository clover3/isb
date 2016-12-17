package com.postech.isb.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created by newmbewb on 2016-12-17.
 */
public class HeartbeaterReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Messenger heartbeatMessageHandler;
        Log.i("Heartbeater", "doodoom-cheet (recver)!");
        Bundle extras = intent.getExtras();
        heartbeatMessageHandler = (Messenger) extras.get("MESSENGER");
        try {
            heartbeatMessageHandler.send(new Message());
        }
        catch (RemoteException e){
            Log.i("Heartbeater", "Failed to send a message!");
            // Cancel the alarm
            AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            int alarmId = intent.getExtras().getInt("alarmId");
            PendingIntent alarmIntent;
            alarmIntent = PendingIntent.getBroadcast(context, alarmId,
                    new Intent(context, HeartbeaterReceiver.class),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            service.cancel(alarmIntent);
        }
    }
}
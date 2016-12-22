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

import com.postech.isb.R;

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

            // Re-register the heartbeat
            int alarmId = intent.getExtras().getInt("alarmId");
            AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Intent alarm = new Intent(context, HeartbeaterReceiver.class);
            alarm.putExtra("MESSENGER", heartbeatMessageHandler);
            alarm.putExtra("alarmId", alarmId); /* So we can catch the id on BroadcastReceiver */
            //TODO configure your intent
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, alarmId, alarm, PendingIntent.FLAG_UPDATE_CURRENT);
            // XXX: use setExact for API 19 or later.
            alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + context.getResources().getInteger(R.integer.heartbeat_period), alarmIntent);
        }
        catch (RemoteException e){
            Log.i("Heartbeater", "Failed to send a message!");
        }
    }
}
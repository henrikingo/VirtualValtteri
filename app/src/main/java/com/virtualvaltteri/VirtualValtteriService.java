package com.virtualvaltteri;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.virtualvaltteri.sensors.Collect;

public class VirtualValtteriService extends Service {
    public static String CHANNEL_ID ="VirtualValtteri";
    public int startId;
    public Collect collect;
    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.collect = Collect.getInstance(getApplicationContext());

        // Hurry up with that sticky notification
        if(this.collect.notification==null)
            this.collect.standby(this);

        String type = intent.getStringExtra("type");
        String doWhat = intent.getStringExtra("do");
        System.out.println("type: " + type + "    do:" + doWhat);

        if(type.equals("com.virtualvaltteri.VirtualValtteriService")){
            if(doWhat.equals("start")){
                // This might be redundant? Well it causes app to start I guess...
            }
        }
        if(type.equals("com.virtualvaltteri.VirtualValtteriService.sensors")){
            if(doWhat.equals("start")){
                collect.startSensors();
            }
            if(doWhat.equals("stop")){
                collect.stopSensors();
            }
        }
        if(type.equals("com.virtualvaltteri.VirtualValtteriService.race")){
            if(doWhat.equals("start")){
                collect.raceStarted();
            }
            if(doWhat.equals("stop")){
                collect.raceStopped();
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        collect.stopSensors();
        collect.stopNotificationTimer();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
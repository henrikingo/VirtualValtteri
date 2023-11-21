package com.virtualvaltteri.sensors;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.virtualvaltteri.VirtualValtteriService;

public class CollectFg {
    Context context;
    Collect collect;
    public CollectFg(Context context) {
        this.context = context;
        this.collect = Collect.getInstance(context);
    }

    public void startServiceStandby() {
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        serviceIntent.putExtra("type", "com.virtualvaltteri.VirtualValtteriService");
        serviceIntent.putExtra("do", "start");
        context.getApplicationContext().startForegroundService(serviceIntent);
    }
    public void stopService() {
        stopSensors();
        // I think the idea is that an empty intent means there's nothing to do?
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        //context.stopService(serviceIntent);
        context.getApplicationContext().stopService(serviceIntent);
    }

    public void startSensors(){
        startServiceStandby();
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        serviceIntent.putExtra("type", "com.virtualvaltteri.VirtualValtteriService.sensors");
        serviceIntent.putExtra("do", "start");
        //context.startForegroundService(serviceIntent);
        context.getApplicationContext().startForegroundService(serviceIntent);
    }

    public void stopSensors(){
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        serviceIntent.putExtra("type", "com.virtualvaltteri.VirtualValtteriService.sensors");
        serviceIntent.putExtra("do", "stop");
        //context.startForegroundService(serviceIntent);
        context.getApplicationContext().startForegroundService(serviceIntent);
    }

    public void applyPreferences(){
        System.out.println("CollectFg.applyPreferences()");
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        serviceIntent.putExtra("type", "com.virtualvaltteri.VirtualValtteriService.preferences");
        serviceIntent.putExtra("do", "apply");
    }

    public void ping(){
        System.out.println("CollectFg.ping()");
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        serviceIntent.putExtra("type", "com.virtualvaltteri.VirtualValtteriService.foregroundPing");
        serviceIntent.putExtra("do", "ping");
    }

}

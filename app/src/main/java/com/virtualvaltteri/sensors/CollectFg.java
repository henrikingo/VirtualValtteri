package com.virtualvaltteri.sensors;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.virtualvaltteri.VirtualValtteriService;

public class CollectFg {
    Context context;
    Context mainActivity;
    Collect collect;
    public CollectFg(Context context) {
        this.mainActivity = context;
        this.context = mainActivity.getApplicationContext();
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
    public void close(){
        System.out.println("CollectFg.close()");
        if(collect!=null && collect.VVS!=null) collect.VVS.shutdown=true;
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        serviceIntent.putExtra("type", "com.virtualvaltteri.VirtualValtteriService.system");
        serviceIntent.putExtra("do", "close");

    }

}

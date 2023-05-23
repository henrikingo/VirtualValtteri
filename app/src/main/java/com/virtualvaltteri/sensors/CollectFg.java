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
        //context.startForegroundService(serviceIntent);
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

    public void raceStarted(){
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        serviceIntent.putExtra("type", "com.virtualvaltteri.VirtualValtteriService.race");
        serviceIntent.putExtra("do", "start");
    }

    public void raceStopped(){
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        serviceIntent.putExtra("type", "com.virtualvaltteri.VirtualValtteriService.race");
        serviceIntent.putExtra("do", "stop");
    }

    public void applyPreferences(){
        Intent serviceIntent = new Intent(this.context, VirtualValtteriService.class);
        serviceIntent.putExtra("type", "com.virtualvaltteri.VirtualValtteriService.preferences");
        serviceIntent.putExtra("do", "apply");
    }


    public SensorWrapper getSensor(int type){
        return collect.getSensor(type);
    }
}

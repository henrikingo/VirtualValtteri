package com.virtualvaltteri.sensors;

import static android.content.Context.MODE_PRIVATE;
import static com.virtualvaltteri.MainActivity.SHARED_PREFS_MAGIC_WORD;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.format.DateFormat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.opencsv.CSVWriter;
import com.virtualvaltteri.R;
import com.virtualvaltteri.VirtualValtteriService;
import com.virtualvaltteri.settings.SettingsActivity;
import com.virtualvaltteri.vmkarting.MessageHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Collect implements SensorEventListenerWrapper {
//    final public int samplingPeriod = 100000;
    final public int samplingPeriod = SensorManager.SENSOR_DELAY_FASTEST;
    private static SensorManagerWrapper sensorManager;
    private SensorWrapper earthAccel;
    private SensorWrapper rotation;
    private SensorWrapper accel;
    private SensorWrapper gyro;
    private SensorWrapper race;

    //public ConcurrentLinkedDeque<SensorEventWrapper> data;
    public List<SensorEventWrapper> data;
    CSVWriter writer = null;
    String filename;
    String fullPath=null;
    public Notification notification = null;
    public NotificationCompat.Builder builder;
    public final int csvCols=22;
    public final String[] csvColumnNames = {
            "timestamp","type","sensortype","vendor","version","x","y","z","zz","quality",
            "pad11","pad12","pad13","pad14","pad15","pad16","pad17","pad18","pad19","pad20","pad21","pad22"
    };
    private PowerManager.WakeLock wakeLock;
    NotificationChannel channel;
    public Service service;
    Context context;
    CharSequence startTime = null;
    Date startTimeObj = null;
    public boolean started = false;
    public VirtualValtteriService VVS;
    private LooperThread looper;
    private static Collect singletonInstance;

    public static Collect getInstance(Context context){
        if(singletonInstance==null){
            singletonInstance = new Collect(context);
        }
        return singletonInstance;
    }

    public Collect(Context context) {
        this.context = context;
        sensorManager = SensorManagerWrapper.getInstance(context);
        earthAccel = sensorManager.getDefaultSensor(SensorWrapper.TYPE_EARTH_ACCELERATION);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        race = sensorManager.getDefaultSensor(SensorWrapper.TYPE_RACE_EVENT);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);


        //for(SensorWrapper s: sensorManager.getSensorList(Sensor.TYPE_ALL)){
        //    System.out.println(String.format("Listing all sensors: typeString=%s type=%s vendor=%s", s.getStringType(), s.getType(), s.getVendor()));
        //}


        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VirtualValtteri::SensorsStarted");

        // Make sure looper is setup before we unleash the sensor event listeners
        looper = new LooperThread();
        looper.start();
        // This became some kind of main event loop when we moved into being a service... oops \o/
        startVvsEventLoop();
    }

    public void startVvsEventLoop(VirtualValtteriService vvs){
        this.VVS=vvs;
        startVvsEventLoop();
    }
    public void startVvsEventLoop(){
        startNotificationTimer();
        createNotificationChannel();
    }

    public void raceStarted(){
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        String settingMode = prefs.getString("collect_sensor_data_key", "race");
        System.out.println("Read setting Collect.mode: " + settingMode);
        if(settingMode.equals("race")){
            if(started) {
                stopSensors();
            }
            startSensors();
            idleCount=0;
        }
    }
    public void raceStopped(){
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        String settingMode = prefs.getString("collect_sensor_data_key", "race");
        System.out.println("Read setting Collect.mode: " + settingMode);
        if(settingMode.equals("race")){
            stopSensors();
            idleCount=0;
        }
    }
    public boolean startSensors() {
        if(started){
            System.out.println("Collect.start: sensors already started, not starting them again.");
            return false;
        }

        //data = new ConcurrentLinkedDeque<>(new ArrayList<SensorEventWrapper>(2*60*(samplingPeriod/1000/1000)));
        data = new ArrayList<SensorEventWrapper>(2*60*(samplingPeriod/1000/1000));
        startTimeObj = new Date();
        startTime = DateFormat.format("yyyy-MM-dd-hh-mm-s", startTimeObj);
        filename = "vmkarting." + startTime + ".csv";
        fullPath = (context.getExternalFilesDir("sensordata").getAbsolutePath() + "/" + filename);
        try {
            System.out.println("Writing sensor data to file: " + fullPath);
            writer = new CSVWriter(new FileWriter(fullPath));
            writer.writeNext(csvColumnNames);
        } catch (IOException e) {
            System.out.println("Cannot open file " + fullPath);
            System.out.println("Will not collect sensor metrics but other than that you can continue as before.");
            e.printStackTrace();
            started = false;
            return false;
        }

        sensorManager.registerListener(this, earthAccel, samplingPeriod);
        sensorManager.registerListener(this, accel, samplingPeriod);
        sensorManager.registerListener(this, gyro, samplingPeriod);
        sensorManager.registerListener(this, rotation, samplingPeriod);
        sensorManager.registerListener(this, race, samplingPeriod);
        started = true;
        startNotificationTimer();
        showCollectNotification(filename);

        wakeLock.acquire();
        return true;
    }
    public boolean stopSensors() {
        //stopNotificationTimer();
        System.out.println("Stop all sensors (whether they were on or not...)");
        sensorManager.flush(this);
        sensorManager.unregisterListener(this);
        if(started){
            wakeLock.release();
        }
        started=false;
        sensorManager.flush(this);

        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            if(!e.getMessage().equals("Stream closed"))
                e.printStackTrace();
        }
        started = false;
        stopNotification();
        return started;
    }

    private int idleCount=0;
    private final int IDLE_MAX = 600;

    public boolean startStopSensors(){
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        if(prefs.contains("collect_sensor_data_key")){
            if(prefs.getString("collect_sensor_data_key", "off").equals("on") && !started){
                startSensors();
                idleCount=0;
                return true;
            }
            if(prefs.getString("collect_sensor_data_key", "off").equals("off") && started){
                stopSensors();
                idleCount=0;
                return true;
            }
            if(!started){
                idleCount++;
                if (idleCount>IDLE_MAX) {
                    System.out.println("Stopping VirtualValtteri after " + IDLE_MAX + " seconds.");
                    idleCount=0;
                    stopVvsService();
                }
                return true;
            }
        }
        return false;
    }

    public void stopVvsService () {
        service.stopForeground(true);
        whichNotification = null;
        notification = null;
        stopNotificationTimer();
        this.VVS.stopWebsocketManager();
        this.service.stopSelf(this.VVS.startId);
    }

    public void onAccuracyChanged(SensorWrapper sensor, int accuracy){
        System.out.println(String.format("Accuracy changed. Sensor=%s accuracy=%s", sensor.getStringType(), accuracy));
    }

    public void onSensorChanged(SensorEventWrapper event) {
        Message msg = new Message();
        msg.obj = event;
        looper.mHandler.sendMessage(msg);
    }

    class LooperThread extends Thread {
        public Handler mHandler;
        private int sampler=0;
        public void run() {
            Looper.prepare();

            mHandler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    addValtteriEvent((SensorEventWrapper) msg.obj);
                    sampler++;
                    if(sampler%1000==0){
                        //System.out.println("Sampling every 1000 sensor events: " + msg + " " + ((SensorEventWrapper)msg.obj));

                    }
                }
            };

            Looper.loop();
        }
    }

    Timer timer;
    TimerTask timerTask;
    public void startNotificationTimer() {
        System.out.println("startNotificationTimer() " + timer);
        if(timer==null){
            timer = new Timer();
            initializeTimerTask();
            //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
            timer.schedule(timerTask, 5000, 10000); //
        }
    }

    public void stopNotificationTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                startStopSensors();
                updateNotification();
                VVS.applyPreferences();
            }
        };
    }

    public void addValtteriEvent(SensorEventWrapper event) {
        if (event.values == null) {
            System.out.println("What kind of event has no values? " + event.sensor.getStringType());
            return;
        }
        if(!started){
            //System.out.println("Discarding incoming Sensor data because Collect.started is false.");
            //System.out.println(event);
            return;
        }


        data.add(event);
        writeCSV(event);
    }
    private void writeCSV(SensorEventWrapper event){
        if (event.sensor == null) {
            System.out.println("What kind of event has no sensor? " + event.values);
            return;
        }
        String[] csvStrings = new String[csvCols];
        csvStrings[0] = ""+event.timestamp;
        csvStrings[1] = ""+event.sensor.getType();
        csvStrings[2] = ""+event.sensor.getStringType();
        csvStrings[3] = event.sensor.getVendor();
        csvStrings[4] = ""+event.sensor.getVersion();
        int i = 0;
        for(i = 0; i<event.values.length; i++)
            csvStrings[i+5] = ""+event.values[i];
        int j=0;
        for(j = 0; j<event.stringValues.length; j++)
            csvStrings[j+i+5] = ""+event.stringValues[j];
        // Padding to even length
        for(int k=j+i+5;k<csvCols;k++){
            csvStrings[k]="";
        }

        //System.out.println(String.join(",",csvStrings));
        writer.writeNext(csvStrings, false);
    }

    protected void onDestroy() {
        looper.mHandler.getLooper().quit();
        try {
            if(writer!=null) {
                writer.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "VirtualValtteri sensors";
            String description = "VirtualValtteri shows notification when it is collecting sensor data.";

            int importance = NotificationManager.IMPORTANCE_HIGH;
            channel = new NotificationChannel("VirtualValtteri", "VirtualValtteri sensors" ,importance);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.createNotificationChannel(channel);
        }
    }


    public void standby(Service service){
        this.service = service;
        createNotificationChannel();

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        String settingMode = prefs.getString("collect_sensor_data_key", "race");
        System.out.println("Read setting Collect.mode: " + settingMode);
        if(settingMode.equals("on")) startSensors();
        if(settingMode.equals("off")) stopSensors();
        if(settingMode.equals("race")){
            if(started){
                startSensors();
            }
            else {
                stopSensors();
            }
        }

        createStandbyNotification();
    }
    public void createStandbyNotification(){
        System.out.println("createStandbyNotification()");

        if(builder==null)
            builder = new NotificationCompat.Builder(context, "VirtualValtteri");

        builder.setSmallIcon(R.drawable.racinghelmet_small_notification)
                .setContentTitle("Standby...")
                .setContentText("Sensors stopped")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setColorized(true)
                .setLights(Color.rgb(0xff,0xda, 0xa9),200,800)
                .setOngoing(true)
                .setSilent(false)
                .setAutoCancel(false)
                .setColor(Color.rgb(0xff,0xaa, 0x55));

        notification = builder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        //Intent notificationIntent = new Intent(service, MainActivity.class);
        //PendingIntent pendingIntent = PendingIntent.getActivity(service,0, notificationIntent, 0);
        service.startForeground(77, notification);
    }
    public void showCollectNotification(String filename){
        System.out.println("showCollectNotification()");
        //createNotificationChannel();

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        if(builder==null)
            builder = new NotificationCompat.Builder(context, "VirtualValtteri");

        builder.setSmallIcon(R.drawable.racinghelmet_small_notification)
                .setContentTitle(filename)
                .setContentText(getNotificationString())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setColorized(true)
                .setContentInfo("Info?")
                .setLights(Color.rgb(0xff,0xda, 0xa9),200,800)
                .setOngoing(true)
                .setSilent(false)
                .setTicker("ticker")
                .setAutoCancel(false)
                .setColor(Color.rgb(0xff,0xaa, 0x55))
                .setOnlyAlertOnce(true)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent);

        notification = builder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        //notificationManager.getNotificationChannel("VirtualValtteri");
        //notificationManager.notify(77, notification);
        service.startForeground(77, notification);
    }
    public String getNotificationString(){
        long size=-1;
        Path path = Paths.get(fullPath);
        try {
            size = Files.size(path);
        }
        catch(IOException e){
            System.out.println(e);
            e.printStackTrace();
        }
        Date now = new Date();
        double timeDiff = (now.getTime() - startTimeObj.getTime()) / 1000.0;
        int minutes = ((int)timeDiff / 60);
        int seconds = ((int)timeDiff) % 60;

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.getDefault());

        String content = formatter.format("%dm%ds %d events %.2f MB", minutes, seconds, data.size(), size/1024.0/1024.0).toString();
        return content;
    }
    public void updateNotification() {
        if(started)
            showStartedNotification();
        else
            showStandbyNotification();
    }

    private String whichNotification = null;
    public void showStartedNotification () {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if(notification!=null){
            builder
                    .setContentTitle(filename)
                    .setContentText(getNotificationString());
            notification = builder.build();
            notificationManager.notify(77, notification);
        }
        else {
            System.out.println("notification was null");
        }
        whichNotification="started";
    }
    public void showStandbyNotification () {
        if(whichNotification=="standby") return;

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if(notification!=null){
            builder
                    .setContentTitle("Standby...")
                    .setContentText("Sensors stopped");
            notification = builder.build();
            notificationManager.notify(77, notification);
        }
        else {
            System.out.println("notification was null");
        }
        whichNotification="standby";
    }

    public void stopNotification(){
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(77);
        showStandbyNotification();
    }
    public SensorWrapper getSensor(int type){
        return sensorManager.getDefaultSensor(type);
    }
    public void ping(){
        idleCount=0;
    }
}

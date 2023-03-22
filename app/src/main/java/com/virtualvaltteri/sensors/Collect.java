package com.virtualvaltteri.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;

import com.opencsv.CSVWriter;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Collect implements SensorEventListenerWrapper {
//    final public int samplingPeriod = 100000;
    final public int samplingPeriod = SensorManager.SENSOR_DELAY_FASTEST;
    private SensorManagerWrapper sensorManager;
    private SensorWrapper earthAccel;
    private SensorWrapper rotation;
    private SensorWrapper race;


    //public ConcurrentLinkedDeque<SensorEventWrapper> data;
    public List<SensorEventWrapper> data;
    CSVWriter writer = null;
    Context context;
    CharSequence startTime = null;
    public boolean started = false;
    private LooperThread looper;
    private static Collect singletonInstance;

    public Collect getInstance(Context context){
        if(singletonInstance==null){
            singletonInstance = new Collect(context);
        }
        return singletonInstance;
    }

    public Collect(Context context){
        this.context = context;
        sensorManager = SensorManagerWrapper.getInstance(context);
        earthAccel = sensorManager.getDefaultSensor(SensorWrapper.TYPE_EARTH_ACCELERATION);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        race = sensorManager.getDefaultSensor(SensorWrapper.TYPE_RACE_EVENT);
;

        for(SensorWrapper s: sensorManager.getSensorList(Sensor.TYPE_ALL)){
            System.out.println(String.format("Listing all sensors: typeString=%s type=%s vendor=%s", s.getStringType(), s.getType(), s.getVendor()));
        }

        // Make sure looper is setup before we unleash the sensor event listeners
        looper = new LooperThread();
        looper.start();

        SharedPreferences prefs = context.getSharedPreferences("com.virtualvaltteri", Context.MODE_PRIVATE);
        String settingMode = prefs.getString("collect_sensor_data_key", "race");
        System.out.println("Read setting Collect.mode: " + settingMode);
        if(settingMode.equals("on")) startSensors();
        if(settingMode.equals("off")) stopSensors();
    }

    public void raceStarted(){
        SharedPreferences prefs = context.getSharedPreferences("com.virtualvaltteri", Context.MODE_PRIVATE);
        String settingMode = prefs.getString("collect_sensor_data_key", "race");
        System.out.println("Read setting Collect.mode: " + settingMode);
        if(settingMode.equals("race")){
            if(started) {
                stopSensors();
            }
            startSensors();
        }
    }
    public void raceStopped(){
        SharedPreferences prefs = context.getSharedPreferences("com.virtualvaltteri", Context.MODE_PRIVATE);
        String settingMode = prefs.getString("collect_sensor_data_key", "race");
        System.out.println("Read setting Collect.mode: " + settingMode);
        if(settingMode.equals("race")){
            stopSensors();
        }
    }
    public boolean startSensors() {
        if(started){
            System.out.println("Collect.start: sensors already started, not starting them again.");
            return false;
        }

        //data = new ConcurrentLinkedDeque<>(new ArrayList<SensorEventWrapper>(2*60*(samplingPeriod/1000/1000)));
        data = new ArrayList<SensorEventWrapper>(2*60*(samplingPeriod/1000/1000));
        startTime = DateFormat.format("yyyy-MM-dd-hh-mm-s", new Date());
        String fileName = "VirtualValtteri.vmkarting." + startTime + ".csv";
        try {
            fileName = (context.getExternalFilesDir("sensordata").getAbsolutePath() + "/" + fileName);
            System.out.println("Writing sensor data to file: " + fileName);
            writer = new CSVWriter(new FileWriter(fileName));
        } catch (IOException e) {
            System.out.println("Cannot open file " + fileName);
            System.out.println("Will not collect sensor metrics but other than that you can continue as before.");
            e.printStackTrace();
            started = false;
            return false;
        }

        sensorManager.registerListener(this, earthAccel, samplingPeriod);
        sensorManager.registerListener(this, rotation, samplingPeriod);
        sensorManager.registerListener(this, race, samplingPeriod);
        started = true;

        return true;
    }
    public boolean stopSensors() {
        System.out.println("Stop all sensors (whether they were on or not...)");
        sensorManager.flush(this);
        sensorManager.unregisterListener(this);
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
        if(started){
            String fileName = "VirtualValtteri.vmkarting." + startTime + ".java.ArrayList.serialized";
            try {
                fileName = (context.getExternalFilesDir("sensordata").getAbsolutePath() + "/" + fileName);
                System.out.println("Dumping sensor data to file: " + fileName);
                FileOutputStream fos = new FileOutputStream(fileName);
                ObjectOutputStream os = new ObjectOutputStream(fos);
                os.writeObject(data);
                os.flush();
                os.close();
            } catch (IOException e) {
                System.out.println("Cannot open file " + fileName);
                System.out.println("Failed to dump sensor data as binary (Serialized) file. Your CSV file might be good though?");
                e.printStackTrace();
                return false;
            } finally {
                started = false;
            }
        }
        started = false;
        return started;
    }


    public void onAccuracyChanged(SensorWrapper sensor, int accuracy){
        System.out.println(String.format("Accuracy changed. Sensor=%s accuracy=%s", sensor.getStringType(), accuracy));
    }

    public void onSensorChanged(SensorEventWrapper event) {
        Message msg = new Message();
        msg.obj = event;
        looper.mHandler.sendMessage(msg);
    }

    private SensorEventWrapper previous_accelerometer;
    private SensorEventWrapper previous_acceleration;

    class LooperThread extends Thread {
        public Handler mHandler;
        private int sampler=0;

        public void run() {
            Looper.prepare();

            mHandler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    //System.out.println("handleMessage" + msg.obj);
                    // System.out.print(((SensorEventWrapper)msg.obj).sensor.getStringType() + " ");

                    addValtteriEvent((SensorEventWrapper) msg.obj);
                    sampler++;
                    if(sampler%1000==0){
                        //System.out.println("Sampling every 1000 sensor events: " + msg + " " + ((SensorEventWrapper)msg.obj));

                    }
                    SensorEventWrapper sensorEvent = (SensorEventWrapper)msg.obj;
                    if(sensorEvent.sensor.TYPE==Sensor.TYPE_ACCELEROMETER){
                        previous_accelerometer=sensorEvent;
                    }
                }
            };

            Looper.loop();
        }
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
        int arrayLength = event.values.length + event.stringValues.length + 5;
        String[] csvStrings = new String[arrayLength];
        csvStrings[0] = ""+event.timestamp;
        csvStrings[1] = ""+event.sensor.getType();
        csvStrings[2] = ""+event.sensor.getStringType();
        csvStrings[3] = event.sensor.getVendor();
        csvStrings[4] = ""+event.sensor.getVersion();
        int i = 0;
        for(i = 0; i<event.values.length; i++)
            csvStrings[i+5] = ""+event.values[i];
        for(int j = 0; j<event.stringValues.length; j++)
            csvStrings[j+i+5] = ""+event.stringValues[j];

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


    public SensorWrapper getSensor(int type){
        return sensorManager.getDefaultSensor(type);
    }
}

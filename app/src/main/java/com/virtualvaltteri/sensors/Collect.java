package com.virtualvaltteri.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
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
    final public int samplingPeriod = 100000;
    private SensorManagerWrapper sensorManager;
    private SensorWrapper acceleration;
    private SensorWrapper magnetic;
    private SensorWrapper magneticRotation;
    private SensorWrapper gyro;

    private SensorWrapper rotation;
    private SensorWrapper compassNorth;
    private SensorWrapper temperature;
    private SensorWrapper humidity;
    private SensorWrapper pressure;
    private SensorWrapper light;
    private SensorWrapper[] allSensors;
    private SensorWrapper velocity;
    private SensorWrapper position;
    private CompositeRotationSensor compositeRotation;
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
        //sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager = SensorManagerWrapper.getInstance(context);
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magneticRotation = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        acceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        velocity = (VelocitySensor) sensorManager.getDefaultSensor(SensorWrapper.TYPE_VELOCITY);
        position = (PositionSensor) sensorManager.getDefaultSensor(SensorWrapper.TYPE_POSITION);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        //compassNorth = sensorManager.getDefaultSensor(Sensor.TYPE_HEADING);
        //temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        //humidity = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        //pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        //compositeRotation = new CompositeRotationSensor(sensorManager, this);
        compositeRotation = (CompositeRotationSensor) sensorManager.getDefaultSensor(SensorWrapper.TYPE_COMPOSITE_ROTATION);

        allSensors = new SensorWrapper[]{magnetic, gyro, magneticRotation, acceleration, velocity, position, rotation, light};

        for(SensorWrapper s: sensorManager.getSensorList(Sensor.TYPE_ALL)){
            System.out.println(String.format("Listing all sensors: typeString=%s type=%s vendor=%s", s.getStringType(), s.getType(), s.getVendor()));
        }

        // Make sure looper is setup before we unleash the sensor event listeners
        looper = new LooperThread();
        looper.start();

        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences" ,Context.MODE_PRIVATE);
        String settingMode = prefs.getString("collect_sensor_data_key", "race");
        System.out.println("Read setting Collect.mode: " + settingMode);
        if(settingMode.equals("on")) startSensors();
        if(settingMode.equals("off")) stopSensors();
    }

    public void raceStarted(){
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences" ,Context.MODE_PRIVATE);
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
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences" ,Context.MODE_PRIVATE);
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

        sensorManager.registerListener(this, acceleration, samplingPeriod);
        sensorManager.registerListener(this, rotation, samplingPeriod);
        sensorManager.registerListener(this, magnetic, samplingPeriod);
        sensorManager.registerListener(this, gyro, samplingPeriod);
        sensorManager.registerListener(this, magneticRotation, samplingPeriod);
        //sensorManager.registerListener(this, compassNorth, 10*1000*1000);
        //sensorManager.registerListener(this, temperature, 1000*1000);
        //sensorManager.registerListener(this, humidity, 1000*1000);
        //sensorManager.registerListener(this, pressure, 1000*1000);
        sensorManager.registerListener(this, light, 1000*1000);
        sensorManager.registerListener(this,velocity,samplingPeriod);
        sensorManager.registerListener(this, position, samplingPeriod);
        sensorManager.registerListener(this, compositeRotation, samplingPeriod);
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
            e.printStackTrace();
        }
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
    class LooperThread extends Thread {
        public Handler mHandler;

        public void run() {
            Looper.prepare();

            mHandler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    //System.out.println("handleMessage" + msg.obj);
                    // System.out.print(((SensorEventWrapper)msg.obj).sensor.getStringType() + " ");

                    addValtteriEvent((SensorEventWrapper) msg.obj);
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
//        System.out.println(event.sensor.type);
//        System.out.println(event.sensor.TYPE);

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

package com.virtualvaltteri.sensors;

import android.hardware.Sensor;

import java.io.Serializable;
import java.util.ArrayList;

public class SensorWrapper implements Serializable {
    public static final int TYPE_CUSTOM_SENSOR_BASE = Sensor.TYPE_DEVICE_PRIVATE_BASE + 69;
    public static final int TYPE_RACE_EVENT = TYPE_CUSTOM_SENSOR_BASE + 1;
    public static final int TYPE_VELOCITY = TYPE_CUSTOM_SENSOR_BASE + 2;
    public static final int TYPE_POSITION = TYPE_CUSTOM_SENSOR_BASE + 3;
    public static final int TYPE_COMPOSITE_ROTATION = TYPE_CUSTOM_SENSOR_BASE + 4;
    public static final int TYPE_ROTATION_MATRIX = TYPE_CUSTOM_SENSOR_BASE + 5;
    public static final int TYPE_CUSTOM_SENSOR_MAX = TYPE_CUSTOM_SENSOR_BASE + 5;
    public int TYPE = -1;
    public String type = "Generic SensorWrapper - Please override/overwrite this string and TYPE in your constructor";

    public String vendor = "virtualvaltteri";
    public int version = 1;
    public boolean isAndroidSensor = false;

    ArrayList<SensorEventListenerWrapper> listeners;

    public SensorWrapper(){
        listeners = new ArrayList<>(2);
    }
    public SensorWrapper(Sensor sensor){
        this();
        this.TYPE = sensor.getType();
        this.type = sensor.getStringType();
        this.vendor = sensor.getVendor();
        this.version = sensor.getVersion();
    }
    public SensorWrapper(SensorWrapper sensor){
        this();
        this.TYPE = sensor.getType();
        this.type = sensor.getStringType();
        this.vendor = sensor.getVendor();
        this.version = sensor.getVersion();
    }
    public  int getType(){
        //assert TYPE > SensorWrapper.TYPE_CUSTOM_SENSOR_BASE;
        return TYPE;
    }
    public void setType(int TYPE){
        this.TYPE=TYPE;
    }
    public int getVersion(){
        return version;
    }
    public void setVersion(int version){
        this.version=version;
    }
    public String getVendor(){
        return vendor;
    }
    public void setVendor(String vendor){
        this.vendor=vendor;
    }
    public String getStringType(){
        return this.type;
    }
    public void setStringType(String type){
        this.type=type;
    }
    public void setType(String type) {
        this.type = type;
    }

    protected void registerListener(SensorEventListenerWrapper listener, int samplingPeriod) {
        listeners.add(listener);
    }
    protected void unregisterListener(SensorEventListenerWrapper listener){
        if(listeners.contains(listener)) listeners.remove(listener);
    }

    public SensorEventWrapper triggerEvent(String s){
        SensorEventWrapper event = new SensorEventWrapper(this, s);
        genericEventHandler(event);
        return event;
    }

    protected SensorEventWrapper genericEventHandler(SensorEventWrapper event){
        for(SensorEventListenerWrapper listener: listeners){
            listener.onSensorChanged(event);
        }
        return event;
    }

    protected void triggerEvent(SensorEventWrapper newVelocity) {
        genericEventHandler(newVelocity);
    }

    /**
     * Just returns true. Subclasses should override this to do anything.
     * @param listener
     * @return true
     */
    public boolean flush(SensorEventListenerWrapper listener){
        return true;
    }
}

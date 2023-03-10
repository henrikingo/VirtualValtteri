package com.virtualvaltteri.sensors;

import android.hardware.SensorEvent;
import android.os.SystemClock;

import java.io.Serializable;
import java.util.Arrays;

public class SensorEventWrapper
        implements Serializable {
    public SensorWrapper sensor;
    public float [] values;
    public String [] stringValues;
    long timestamp = 0;
    public SensorEventWrapper(){
        if(this.values==null) this.values = new float[9];
        if(this.stringValues==null) this.stringValues = new String[6];
        this.timestamp = SystemClock.elapsedRealtimeNanos();
    }
    public SensorEventWrapper(SensorWrapper sensor){
        this();
        assert sensor!=null;
        // Easier for everyone to not leave Nulls hanging around
        this.sensor = new SensorWrapper();
        this.sensor.type = sensor.getStringType();
        this.sensor.TYPE = sensor.getType();
        this.sensor.vendor = sensor.getVendor();
        this.sensor.version = sensor.getVersion();
    }
    public SensorEventWrapper(SensorWrapper sensor, String s){
        this(sensor);
        if(this.values==null) this.values = new float[6];
        if(this.stringValues==null) this.stringValues = new String[1];
        stringValues[0] = s;
    }
    public SensorEventWrapper(SensorEventWrapper event){
        this(event.sensor);
        if(event.values!=null) {
            this.values = new float[event.values.length];
            for (int i = 0; i < event.values.length; i++) {
                this.values[i] = event.values[i];
                this.stringValues = new String[1];

            }
        }
        this.timestamp=event.timestamp;
    }

    public float[] getValues() {
        return values;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setValues(float[] values) {
        this.values = new float[values.length];
        System.arraycopy(values,0,this.values, 0,values.length);
    }

    public String[] getStringValues() {
        return stringValues;
    }

    public void setStringValues(String[] stringValues) {
        this.stringValues = new String[stringValues.length];
        System.arraycopy(stringValues,0,this.stringValues, 0,stringValues.length);
    }

    @Override
    public String toString() {
        return "SensorEventWrapper{" +
                "TYPE=" + sensor.TYPE +
                ", type='" + sensor.type + '\'' +
                ", vendor='" + sensor.vendor + '\'' +
                ", version=" + sensor.version +
                ", timestamp=" + timestamp +
                ", values=" + Arrays.toString(values) +
                ", stringValues=" + String.join(",",stringValues) +
                '}';
    }
}

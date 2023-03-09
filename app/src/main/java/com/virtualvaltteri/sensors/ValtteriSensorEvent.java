package com.virtualvaltteri.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.io.Serializable;
import java.util.Arrays;

public class ValtteriSensorEvent
        implements Serializable {
    public int TYPE = -1;
    public String type;
    public String vendor = "virtualvaltteri";
    public int version = 1;
    public float [] values;
    public String [] stringValues;
    long timestamp = 0;
    public ValtteriSensorEvent(){
        if(this.values==null) this.values = new float[9];
        if(this.stringValues==null) this.stringValues = new String[6];
    }

    public ValtteriSensorEvent(SensorEvent event){
        this.TYPE = event.sensor.getType();
        this.type = event.sensor.getStringType();
        this.vendor = event.sensor.getVendor();
        this.version = event.sensor.getVersion();
        this.values = new float[event.values.length];
        for(int i=0; i<event.values.length; i++){
            this.values[i] = event.values[i];
        }
        this.timestamp=event.timestamp;

        // Easier for everyone to not leave Nulls hanging around
        if(this.values==null) this.values = new float[9];
        if(this.stringValues==null) this.stringValues = new String[6];
    }
    public  int getTYPE(){
        assert TYPE > Sensor.TYPE_DEVICE_PRIVATE_BASE;
        return TYPE;
    }
    public void setTYPE(int TYPE){
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

    public float[] getValues() {
        return values;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValues(float[] values) {
        this.values = values;
    }

    public String[] getStringValues() {
        return stringValues;
    }

    public void setStringValues(String[] stringValues) {
        this.stringValues = stringValues;
    }

    @Override
    public String toString() {
        return "ValtteriSensorEvent{" +
                "TYPE=" + TYPE +
                ", type='" + type + '\'' +
                ", vendor='" + vendor + '\'' +
                ", version=" + version +
                ", values=" + Arrays.toString(values) +
                ", timestamp=" + timestamp +
                '}';
    }
}

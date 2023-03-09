package com.virtualvaltteri.sensors;


import android.hardware.Sensor;
import android.os.SystemClock;

import java.io.Serializable;

public class RaceEvent extends SensorEventWrapper implements Serializable {
    public RaceEvent(SensorWrapper sensor,String subType, String s, String s2, String s3, String s4){
        super(sensor);
        if(this.values==null) this.values = new float[6];
        if(this.stringValues==null) this.stringValues = new String[6];
        stringValues[0] = subType;
        stringValues[1] = s;
        stringValues[2] = s2;
        stringValues[3] = s3;
        stringValues[4] = s4;
    }
    public RaceEvent(SensorWrapper sensor, String subType, String s, String s2, String s3){
        this(sensor,subType,s,s2,s3,"");
    }
    public RaceEvent(SensorWrapper sensor, String subType, String s, String s2){
        this(sensor,subType,s,s2,"");
    }
    public RaceEvent(SensorWrapper sensor, String subType, String s){
        this(sensor,subType,s,"");
    }
    public RaceEvent(SensorWrapper sensor, String subType){
        this(sensor,subType,"");
    }
}

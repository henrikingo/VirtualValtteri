package com.virtualvaltteri.sensors;


import android.hardware.Sensor;

import java.io.Serializable;

public class CompositeRotationEvent extends SensorEventWrapper implements Serializable {
    public CompositeRotationEvent(SensorWrapper sensor, float[] gyroOrientation){
        super(sensor);
        this.values = new float[3];
        System.arraycopy(gyroOrientation,0,this.values, 0, this.values.length) ;
    }
}

package com.virtualvaltteri.sensors;


import android.hardware.SensorEvent;

import java.io.Serializable;

public class EarthAccelerationEvent extends SensorEventWrapper implements Serializable {
    public EarthAccelerationEvent(SensorWrapper sensor, float[] values){
        super(sensor);
        this.values = values;
    }
}

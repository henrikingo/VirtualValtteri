package com.virtualvaltteri.sensors;


import android.hardware.SensorEvent;

import java.io.Serializable;

public class AndroidSensorEventWrapper extends SensorEventWrapper implements Serializable {
    public AndroidSensorEventWrapper(SensorEvent event){
        super(new AndroidSensorWrapper(event.sensor));
        if(event.values!=null) {
            this.values = new float[event.values.length];
            for (int i = 0; i < event.values.length; i++) {
                this.values[i] = event.values[i];
                this.stringValues = new String[1];

            }
        }
        this.timestamp=event.timestamp;
    }

}

package com.virtualvaltteri.sensors;


import android.hardware.SensorEvent;

import java.io.Serializable;

public class AndroidSensorEventWrapper extends SensorEventWrapper implements Serializable {
    public AndroidSensorEventWrapper(SensorEvent asEvent){
        super();
        if(asEvent.values!=null) {
            this.values = new float[asEvent.values.length];
            for (int i = 0; i < asEvent.values.length; i++) {
                this.values[i] = asEvent.values[i];
                this.stringValues = new String[1];

            }
        }
        this.timestamp=asEvent.timestamp;
        // os.hardware.Sensor isn't serializable, so fake one for this purpose
        this.sensor = new SensorWrapper();
        this.sensor.type = asEvent.sensor.getStringType();
        this.sensor.TYPE = asEvent.sensor.getType();
        this.sensor.vendor = asEvent.sensor.getVendor();
        this.sensor.version = asEvent.sensor.getVersion();
    }
}

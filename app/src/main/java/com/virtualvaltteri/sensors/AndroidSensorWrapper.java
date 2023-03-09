package com.virtualvaltteri.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;

public class AndroidSensorWrapper extends SensorWrapper {
    final boolean isAndroidSensor = true;
    private Sensor androidSensor;
    public AndroidSensorWrapper(Sensor androidSensor){
        this.type = androidSensor.getStringType();
        this.TYPE = androidSensor.getType();
        this.androidSensor = androidSensor;
    }
    /**
     * Overrides the assertion from the parent class. For this class we want to use the original type numbers.
     */
    @Override
    public int getType() {
        return TYPE;
    }

    public String getStringType(){

        return type;
    }
    Sensor secretGetAndroidSensor(){
        return androidSensor;
    }
    protected void registerListener(SensorEventListener listener, int samplingPeriod) {
        assert false; // Please use SensorManagerWrapper.registerListener()
    }

}

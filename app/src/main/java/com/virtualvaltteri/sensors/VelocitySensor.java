package com.virtualvaltteri.sensors;

import android.hardware.Sensor;

import java.io.Serializable;
import java.util.Arrays;

public class VelocitySensor extends SensorWrapper implements Serializable, SensorEventListenerWrapper {
    private SensorWrapper accelerationSensor;
    private VelocityEvent previousVelocity;
    private SensorManagerWrapper sensorManagerWrapper;

    public VelocitySensor(SensorManagerWrapper manager){
        super();
        this.sensorManagerWrapper = manager;
        accelerationSensor = sensorManagerWrapper.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        TYPE = SensorWrapper.TYPE_VELOCITY;
        type = "Velocity";
    }
    public void registerListener(SensorEventListenerWrapper listener, int samplingPeriod){
        super.registerListener(listener, samplingPeriod);
        sensorManagerWrapper.registerListener( this, accelerationSensor, samplingPeriod);
        if(previousVelocity==null){
            previousVelocity = new VelocityEvent(this);
        }
    }
    public void unregisterListener(SensorEventListenerWrapper listener){
        super.unregisterListener(listener);
        System.out.println("VelocitySensor listeners: " + String.join(",", Arrays.toString(listeners.toArray())));
        if(listeners.isEmpty()){
            accelerationSensor.unregisterListener(this);
        }
    }
    @Override
    public void onSensorChanged(SensorEventWrapper upstreamAcceleration) {
        if (upstreamAcceleration.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            VelocityEvent newVelocity = new VelocityEvent(previousVelocity, ((AndroidSensorEventWrapper)upstreamAcceleration));
            this.triggerEvent(newVelocity);
            previousVelocity = newVelocity;
        }
    }

    @Override
    public void onAccuracyChanged(SensorWrapper sensor, int i) {

    }
}

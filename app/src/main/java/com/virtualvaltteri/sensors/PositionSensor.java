package com.virtualvaltteri.sensors;

import java.io.Serializable;

public class PositionSensor extends SensorWrapper implements Serializable, SensorEventListenerWrapper {
    final private SensorWrapper velocitySensor;
    private PositionEvent previousPosition;
    final private SensorManagerWrapper sensorManagerWrapper;

    public PositionSensor(SensorManagerWrapper manager){
        super();
        TYPE = SensorWrapper.TYPE_POSITION;
        type = "Position";
        this.sensorManagerWrapper = manager;
        velocitySensor = sensorManagerWrapper.getDefaultSensor(SensorWrapper.TYPE_VELOCITY);
    }
    public void registerListener(SensorEventListenerWrapper listener, int samplingPeriod){
        super.registerListener(listener, samplingPeriod);
        sensorManagerWrapper.registerListener( this, velocitySensor, samplingPeriod);
        if(previousPosition==null){
            previousPosition = new PositionEvent(this);
        }
    }
    public void unregisterListener(SensorEventListenerWrapper listener){
        super.unregisterListener(listener);
        if(listeners.isEmpty()){
            velocitySensor.unregisterListener(this);
        }
    }
    @Override
    public void onSensorChanged(SensorEventWrapper upstreamVelocity) {
        if (upstreamVelocity.sensor.getType() == SensorWrapper.TYPE_VELOCITY) {
            PositionEvent newPosition = new PositionEvent(previousPosition, (VelocityEvent) upstreamVelocity);
            this.triggerEvent(newPosition);
            previousPosition = newPosition;
        }
    }

    @Override
    public void onAccuracyChanged(SensorWrapper sensor, int i) {

    }
}

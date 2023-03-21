package com.virtualvaltteri.sensors;

import android.hardware.SensorManager;
import android.os.SystemClock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RotationMatrixSensor extends CompositeRotationSensor implements SensorEventListenerWrapper{
    private Set<SensorManagerWrapper> myListeners = new HashSet<>();
    SensorManagerWrapper sensorManager;
    public RotationMatrixSensor(SensorManagerWrapper sensorManager) {
        super(sensorManager);
        TYPE = SensorWrapper.TYPE_ROTATION_MATRIX;
        type = "RotationMatrix";
    }
    public void registerListener(SensorEventListenerWrapper listener, int samplingPeriod) {
        super.registerListener(this,samplingPeriod);
        registerRotationMatrixListener(this, SensorManager.SENSOR_DELAY_FASTEST);
    }
    public void unregisterListener(SensorEventListenerWrapper listener){
        if(myListeners.contains(listener)){
            myListeners.remove(listener);
            super.unregisterListener(this);
            unregisterRotationMatrixListener(this);
        }
    }


    private Set<SensorEventListenerWrapper> rotationMatrixListeners = new HashSet<>();
    protected void registerRotationMatrixListener(SensorEventListenerWrapper listener, int samplingPeriod) {
        rotationMatrixListeners.add(listener);
    }
    protected void unregisterRotationMatrixListener(SensorEventListenerWrapper listener){
        if(rotationMatrixListeners.contains(listener)) listeners.remove(listener);
    }

    private void triggerRotationMatrixEvent(float[] matrix, long timestamp){
        for(SensorEventListenerWrapper listener: rotationMatrixListeners){
            RotationMatrixEvent rmEvent = new RotationMatrixEvent(this, matrix, timestamp);
            listener.onSensorChanged(rmEvent);
        }
    }

    @Override
    public void onAccuracyChanged(SensorWrapper sensor, int accuracy) {
        // We don't use this anywhere but would be easy to pass through too if there was a similar triggerEvent()
    }

    @Override
    public void onSensorChanged(SensorEventWrapper rotationMatrix) {
        // Just passing through to my listeners
        triggerRotationMatrixEvent(getRotationMatrix(), rotationMatrix.timestamp);
    }
}


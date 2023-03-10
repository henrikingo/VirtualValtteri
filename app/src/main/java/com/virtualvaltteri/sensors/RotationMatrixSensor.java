package com.virtualvaltteri.sensors;

import android.hardware.SensorManager;
import android.os.SystemClock;

public class RotationMatrixSensor extends SensorWrapper implements SensorEventListenerWrapper{
    SensorManagerWrapper sensorManager;
    CompositeRotationSensor compositeRotationSensor;
    public RotationMatrixSensor(SensorManagerWrapper sensorManager) {
        super();
        this.sensorManager = sensorManager;
        TYPE = SensorWrapper.TYPE_ROTATION_MATRIX;
        type = "RotationMatrix";
    }
    public void registerListener(SensorEventListenerWrapper listener, int samplingPeriod) {
        super.registerListener(listener,samplingPeriod);
        compositeRotationSensor = (CompositeRotationSensor) sensorManager.getDefaultSensor(SensorWrapper.TYPE_COMPOSITE_ROTATION);
        compositeRotationSensor.registerRotationMatrixListener(this, SensorManager.SENSOR_DELAY_FASTEST);
    }
    public void unregisterListener(SensorEventListenerWrapper listener){
        if(listeners.contains(listener)) listeners.remove(listener);
        compositeRotationSensor = (CompositeRotationSensor) sensorManager.getDefaultSensor(SensorWrapper.TYPE_COMPOSITE_ROTATION);
        compositeRotationSensor.unregisterRotationMatrixListener(this);
    }

    @Override
    public void onAccuracyChanged(SensorWrapper sensor, int accuracy) {
        // We don't use this anywhere but would be easy to pass through too if there was a similar triggerEvent()
    }

    @Override
    public void onSensorChanged(SensorEventWrapper rotationMatrix) {
        // Just passing through to my listeners
        triggerEvent(rotationMatrix);
    }
}


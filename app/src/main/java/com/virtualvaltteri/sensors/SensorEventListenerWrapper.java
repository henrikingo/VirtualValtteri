package com.virtualvaltteri.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Message;

public interface SensorEventListenerWrapper {
    public void onAccuracyChanged(SensorWrapper sensor, int accuracy);

    public void onSensorChanged(SensorEventWrapper event);
}

package com.virtualvaltteri.sensors;

import android.content.Context;
import android.os.SystemClock;

import com.kircherelectronics.fsensor.observer.SensorSubject;
import com.kircherelectronics.fsensor.sensor.FSensor;
import com.kircherelectronics.fsensor.sensor.acceleration.KalmanLinearAccelerationSensor;

public class FSAccelerationSensor extends SensorWrapper{
    public FSensor fSensor;
    public int fsSamplingsPeriodUs=123456; // magic number
    public long previousTimestamp=0;
    public FSAccelerationSensor(SensorManagerWrapper sensorManager){
        super();
        fSensor = new KalmanLinearAccelerationSensor(sensorManager.context);
        TYPE = SensorWrapper.TYPE_FS_ACCELERATION;
        type = "FSensor.Kalman.Acceleration";
    }

    private SensorSubject.SensorObserver sensorObserver = new SensorSubject.SensorObserver() {
        @Override
        public void onSensorChanged(float[] values) {
            long currentTimestamp = SystemClock.elapsedRealtimeNanos();
            if((currentTimestamp-previousTimestamp)/1000 > fsSamplingsPeriodUs){
                triggerEvent(values, currentTimestamp);
                previousTimestamp=currentTimestamp;
            }
        }
    };
    protected void registerListener(SensorEventListenerWrapper listener, int samplingPeriod) {
        super.registerListener(listener,samplingPeriod);
        if (samplingPeriod<fsSamplingsPeriodUs || fsSamplingsPeriodUs == 12345)
            fsSamplingsPeriodUs=samplingPeriod;
        fSensor.register(sensorObserver);
        fSensor.start();
    }
    protected void unregisterListener(SensorEventListenerWrapper listener){
        super.unregisterListener(listener);
        if(listeners.isEmpty()){
            fSensor.unregister(sensorObserver);
            fSensor.stop();
        }
    }
}

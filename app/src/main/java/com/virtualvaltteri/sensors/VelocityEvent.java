package com.virtualvaltteri.sensors;

import android.hardware.Sensor;

public class VelocityEvent extends SensorEventWrapper {
    /**
     * Only used to initialize a zero position.
     *
     * BY THE WAY note that velocity will by no means be zero when data collection is started.
     * In all likelihood the car is already driving on the track when the race starts, could even be fast.
     * What we can do however: We know that the cars drive in circles, so the sum of all velocity vectors
     * during a lap should be zero!
     */
    public VelocityEvent(SensorWrapper sensor){
        super(sensor);
        values = new float[6];
        for(int i=0;i< values.length;i++){
            values[i]=0;
        }
        if(this.values==null) this.values = new float[6];
        if(this.stringValues==null) this.stringValues = new String[6];
    }
    public VelocityEvent(VelocityEvent previous, SensorEventWrapper acceleration){
        super(previous.sensor);
        this.timestamp = acceleration.timestamp;
        float nanosec = 1000.0F*1000.0F *1000.0F;
        float dt = (float)((acceleration.timestamp - previous.timestamp)/nanosec);
        values = new float[acceleration.values.length];
        for(int i = 0; i< acceleration.values.length; i++){
            // Rounding cuts away small noise when the phone is stationary.
            // The noise would quickly accumulate toward infinity
            // Could round to 0.1 m/s^2 but let's start this crude.
            values[i] = previous.values[i] + acceleration.values[i]*dt/2.0F;
        }
        if(this.values==null) this.values = new float[6];
        if(this.stringValues==null) this.stringValues = new String[6];
    }
    public String getStringType(){
        return "VelocityEvent";
    }
}
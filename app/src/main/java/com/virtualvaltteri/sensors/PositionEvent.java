package com.virtualvaltteri.sensors;

import android.hardware.Sensor;

public class PositionEvent extends SensorEventWrapper {
    /**
     * Only used to initialize a zero position.
     */
    public PositionEvent(SensorWrapper sensor){
        super(sensor);
        values = new float[6];
        for(int i=0;i< values.length;i++){
            values[i]=0;
        }
        if(this.values==null) this.values = new float[6];
        if(this.stringValues==null) this.stringValues = new String[6];
    }
    public PositionEvent(PositionEvent previous, VelocityEvent velocity){
        super(previous.sensor);
        this.timestamp = velocity.timestamp;
        float nanosec = 1000.0F*1000.0F *1000.0F;
        float dt = (float)((velocity.timestamp - previous.timestamp)/nanosec);
        //System.out.println(dt);
        values = new float[velocity.values.length];
        for(int i = 0; i< velocity.values.length; i++){
            values[i] = previous.values[i] + velocity.values[i]*dt/2.0F;
        }
        if(this.values==null) this.values = new float[6];
        if(this.stringValues==null) this.stringValues = new String[6];
    }
    public String getStringType(){
        return "PositionEvent";
    }
}
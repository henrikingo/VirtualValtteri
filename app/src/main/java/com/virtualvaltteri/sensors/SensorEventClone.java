package com.virtualvaltteri.sensors;


import android.hardware.SensorEvent;

import java.io.Serializable;

public class SensorEventClone extends ValtteriSensorEvent implements Serializable {

    public SensorEventClone(){
        super();
        if(this.values==null) this.values = new float[9];
        if(this.stringValues==null) this.stringValues = new String[6];
    }

    public SensorEventClone(SensorEvent event){
        super(event);
        if(this.values==null) this.values = new float[9];
        if(this.stringValues==null) this.stringValues = new String[6];
    }

    /**
     * Overrides the assertion from the parent class. For this class we want to use the original type numbers.
     */
    @Override
    public int getTYPE() {
        return TYPE;
    }

    public String getStringType(){
        return type;
    }
}

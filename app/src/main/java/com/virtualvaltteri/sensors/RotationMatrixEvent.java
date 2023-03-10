package com.virtualvaltteri.sensors;

public class RotationMatrixEvent extends SensorEventWrapper{
    RotationMatrixEvent(SensorWrapper sensor, float[] matrix, long timestamp){
        super(sensor);
        this.timestamp =timestamp;
        this.values = matrix;
    }
}

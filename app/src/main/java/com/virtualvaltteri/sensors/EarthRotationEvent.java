package com.virtualvaltteri.sensors;

import android.hardware.Sensor;
import android.hardware.SensorManager;

public class EarthRotationEvent extends ValtteriSensorEvent {
    /**
     * Only used to initialize a zero position.
     */
    public EarthRotationEvent(SensorEventClone previousAccelerometer, SensorEventClone previousMagnetometer){
        if(previousAccelerometer.timestamp > previousMagnetometer.timestamp){
            TYPE = Sensor.TYPE_DEVICE_PRIVATE_BASE + 75;
            this.timestamp = previousAccelerometer.timestamp;
            this.type = "EarthRotation.From.Accelerometer";
        }
        else {
            TYPE = Sensor.TYPE_DEVICE_PRIVATE_BASE + 76;
            this.timestamp = previousMagnetometer.timestamp;
            this.type = "EarthRotation.From.Magnetometer";
        }
        float[] rotationMatrix = new float[9];
        float[] i = new float[9];

        System.out.println(SensorManager.getRotationMatrix(rotationMatrix, i, previousAccelerometer.values, previousMagnetometer.values))   ;
        System.out.println(""+rotationMatrix[0]+","+rotationMatrix[1]+","+rotationMatrix[2]+","+rotationMatrix[3]+","+rotationMatrix[4]+","+rotationMatrix[5]+","+rotationMatrix[6]+","+rotationMatrix[7]+","+rotationMatrix[8]+",");
        System.out.println(""+i[0]+","+i[1]+","+i[2]+","+i[3]+","+i[4]+","+i[5]+","+i[6]+","+i[7]+","+i[8]+",");
        System.out.println(""+previousAccelerometer.values[0]+","+previousAccelerometer.values[1]+","+previousAccelerometer.values[2]+","+previousAccelerometer.values[3]);
        System.out.println(""+previousMagnetometer.values[0]+","+previousMagnetometer.values[1]+","+previousMagnetometer.values[2]+","+previousMagnetometer.values[3]+",");
        values = new float[3];
        float[] foo = new float[3];
        SensorManager.getOrientation(rotationMatrix, foo);
        System.out.println(""+foo[0]+","+foo[1]+foo[2]);

        if(this.stringValues==null) this.stringValues = new String[6];
    }
    public String getStringType(){
        return type;
    }
}

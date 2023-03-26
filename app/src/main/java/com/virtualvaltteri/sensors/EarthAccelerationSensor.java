package com.virtualvaltteri.sensors;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EarthAccelerationSensor extends SensorWrapper implements SensorEventListenerWrapper {

    public Handler handler;
    public AndroidSensorWrapper linearAccel;
    public AndroidSensorWrapper gravity;
    public AndroidSensorWrapper magnetic;
    private float[] gravityValues = null;
    private float[] magneticValues = null;
    SensorManagerWrapper sensorManager;
    private EarthAccelerationSensor(){
        throw new RuntimeException("EarthAccelerationSensor requires a SensorManagerWrapper object");
    }

    public EarthAccelerationSensor(SensorManagerWrapper sensorManager){
        super();
        this.TYPE=SensorWrapper.TYPE_EARTH_ACCELERATION;
        this.type="Earth.Linear_Acceleration";
        this.linearAccel = (AndroidSensorWrapper) sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        this.gravity = (AndroidSensorWrapper) sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        this.magnetic = (AndroidSensorWrapper) sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        this.sensorManager=sensorManager;

        handler = new Handler(Looper.myLooper()){
            EarthAccelerationSensor outerObject=EarthAccelerationSensor.this;
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if(msg.obj instanceof EarthAccMessage){
                    EarthAccMessage eam = ((EarthAccMessage) msg.obj);
                    outerObject.rotateAndSend((AndroidSensorEventWrapper) eam.acc,eam.gr,eam.mag);
                }
            }
        };
    }


    class EarthAccMessage {
        public SensorEventWrapper acc;
        public float[] gr;
        public float[] mag;
        public EarthAccMessage(AndroidSensorEventWrapper acc, float[] gr, float[] mag){
            this.acc=acc;
            this.gr=gr;
            this.mag=mag;
        }
    }

    public void registerListener(SensorEventListenerWrapper listener, int samplingPeriod) {
        if(listeners.size()==0){
            sensorManager.registerListener(this, linearAccel, samplingPeriod);
            sensorManager.registerListener(this, gravity, samplingPeriod);
            sensorManager.registerListener(this, magnetic, samplingPeriod);
        }
        super.registerListener(listener, samplingPeriod);
    }
    public void unregisterListener(SensorEventListenerWrapper listener){
        super.unregisterListener(listener);
        if(listeners.contains(listener)){
            listeners.remove(listener);
            sensorManager.unregisterListener((SensorEventListenerWrapper) this);
        }
    }

    @Override
    public void onSensorChanged(SensorEventWrapper event) {
        if ((gravityValues != null) && (magneticValues != null)
                && (event.sensor.TYPE == Sensor.TYPE_LINEAR_ACCELERATION)) {

            Message msg = new Message();
            msg.obj = new EarthAccMessage(
                                ((AndroidSensorEventWrapper)event),
                                Arrays.copyOf(this.gravityValues,this.gravityValues.length),
                                Arrays.copyOf(this.magneticValues, this.magneticValues.length)
            );
            // Do work in another thread
            handler.dispatchMessage(msg);

        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            // Apparently Android will reuse the memory space and I'm suppsed to (know that!) and copy into my own buffer/array
            this.gravityValues = Arrays.copyOf(event.values, event.values.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            this.magneticValues = Arrays.copyOf(event.values, event.values.length);
        }

    }

    public void rotateAndSend(AndroidSensorEventWrapper event, float[] gravityValues, float[] magneticValues){


        float[] deviceRelativeAcceleration = new float[4];
        deviceRelativeAcceleration[0] = event.values[0];
        deviceRelativeAcceleration[1] = event.values[1];
        deviceRelativeAcceleration[2] = event.values[2];
        deviceRelativeAcceleration[3] = 0;

        // Change the device relative acceleration values to earth relative values
        // X axis -> East
        // Y axis -> North Pole
        // Z axis -> Sky

        float[] R = new float[16], I = new float[16], earthAcc = new float[4];

        SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

        float[] inv = new float[16];

        android.opengl.Matrix.invertM(inv, 0, R, 0);
        android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);

        triggerEvent(earthAcc, event.timestamp);
    }

    @Override
    public void onAccuracyChanged(SensorWrapper sensor, int i) {

    }
}

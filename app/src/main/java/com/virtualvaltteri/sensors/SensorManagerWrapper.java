package com.virtualvaltteri.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorManagerWrapper {
    private static SensorManagerWrapper singletonInstance;
    public List<SensorWrapper> allReturnedSensors;
    public SensorWrapper[] allCustomSensors = new SensorWrapper[SensorWrapper.TYPE_CUSTOM_SENSOR_MAX-SensorWrapper.TYPE_CUSTOM_SENSOR_BASE];
    public SensorManager androidSensorManager;
    public Map<SensorEventListenerWrapper,AndroidSensorListener> androidSensorListeners;

    public static SensorManagerWrapper getSystemService(Context context){
        return getInstance(context);
    }
    public static SensorManagerWrapper getInstance(Context context){
        if(singletonInstance==null){
            singletonInstance = new SensorManagerWrapper(context);
        }
        return singletonInstance;
    }

    private SensorManagerWrapper(Context context){
        androidSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        allReturnedSensors = new ArrayList<>(20);
        for(int type = SensorWrapper.TYPE_CUSTOM_SENSOR_BASE+1; type <= SensorWrapper.TYPE_CUSTOM_SENSOR_MAX; type++){
            allCustomSensors[idx(type)] = getCustomSensor(type);
        }
        androidSensorListeners = new HashMap<>();
    }
    int idx(int i){
        int rv =i-SensorWrapper.TYPE_CUSTOM_SENSOR_BASE-1;
        //System.out.println(rv);
        return rv;
    }
    public SensorWrapper getDefaultSensor(int type) {
        if(type>SensorWrapper.TYPE_CUSTOM_SENSOR_BASE){
            return allCustomSensors[idx(type)];
        }
        else {
            return new AndroidSensorWrapper(androidSensorManager.getDefaultSensor(type));
        }
    }
    public SensorWrapper getDefaultSensor(int type, boolean wakeUp) {
        if(type>SensorWrapper.TYPE_CUSTOM_SENSOR_BASE){
            SensorWrapper s = getCustomSensor(type, wakeUp);
            assert s!=null;
            //System.out.println(s.getType());
            return s;
        }
        else {
            return new AndroidSensorWrapper(androidSensorManager.getDefaultSensor(type, wakeUp));
        }
    }
    private SensorWrapper getCustomSensor(int type){
        System.out.println("getCustomSensor "+type);
        assert type>SensorWrapper.TYPE_CUSTOM_SENSOR_BASE;
        switch (type){
            case SensorWrapper.TYPE_RACE_EVENT:
                return new RaceEventSensor();
            case SensorWrapper.TYPE_VELOCITY:
                return new VelocitySensor(this);
            case SensorWrapper.TYPE_POSITION:
                return new PositionSensor(this);

        }
        // It's valid to ask for a number that doesn't exist, in which case we return null.
        return null;
    }
    private SensorWrapper getCustomSensor(int type, boolean wakeUp){
        throw new RuntimeException("Not implemented");
    }

    class AndroidSensorListener implements SensorEventListener{
        SensorEventListenerWrapper realListener;
        public AndroidSensorListener(SensorEventListenerWrapper listener){
            this.realListener = listener;
        }
        @Override
        public void onSensorChanged(SensorEvent androidEvent) {
            this.realListener.onSensorChanged(new AndroidSensorEventWrapper(androidEvent));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            this.realListener.onAccuracyChanged(new SensorWrapper(sensor), i);
        }
    }
    public void registerListener(SensorEventListenerWrapper listener, SensorWrapper sensor, int samplingPeriod){
        if(sensor.getType()<SensorWrapper.TYPE_CUSTOM_SENSOR_BASE){
            AndroidSensorWrapper asWrapper = (AndroidSensorWrapper)sensor;
            AndroidSensorListener asListener = new AndroidSensorListener(listener);
            androidSensorListeners.put(listener,asListener);
            androidSensorManager.registerListener((SensorEventListener) asListener, asWrapper.secretGetAndroidSensor(), samplingPeriod);
        }
        else {
            sensor.registerListener(listener, samplingPeriod);
        }
    }
    public void unregisterListener(SensorEventListenerWrapper listener){
        for(SensorWrapper s: allReturnedSensors){
            s.unregisterListener(listener);
        }
        if(androidSensorListeners.containsKey(listener)){
            AndroidSensorListener asListener = androidSensorListeners.get(listener);
            androidSensorManager.unregisterListener(asListener);
            androidSensorListeners.remove(listener);
        }
    }
    public List<SensorWrapper> getSensorList(int type){
        List<Sensor> androidSensors = androidSensorManager.getSensorList(type);
        List<SensorWrapper> returnValues = new ArrayList<>(androidSensors.size());
        for(Sensor as: androidSensors){
            returnValues.add(new AndroidSensorWrapper(as));
        }

        return returnValues;
    }
}

package com.virtualvaltteri.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorManagerWrapper {
    private static SensorManagerWrapper singletonInstance;
    public static List<SensorWrapper> allSensors; // All returned SensorWrapper instances. At most one per type.
    public SensorWrapper[] allCustomSensors = new SensorWrapper[SensorWrapper.TYPE_CUSTOM_SENSOR_MAX-SensorWrapper.TYPE_CUSTOM_SENSOR_BASE];
    public SensorWrapper[] allAndroidSensors = new SensorWrapper[SensorWrapper.TYPE_CUSTOM_SENSOR_BASE];
    public SensorManager androidSensorManager;
    public Map<SensorEventListenerWrapper,List<AndroidSensorListener>> androidSensorListeners;

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
//        for(int type = SensorWrapper.TYPE_CUSTOM_SENSOR_BASE+1; type <= SensorWrapper.TYPE_CUSTOM_SENSOR_MAX; type++){
//            allCustomSensors[idx(type)] = getCustomSensor(type);
//        }
        androidSensorListeners = new HashMap<>();
    }
    int idx(int i){
        int rv =i-SensorWrapper.TYPE_CUSTOM_SENSOR_BASE-1;
        //System.out.println(rv);
        return rv;
    }
    public SensorWrapper getDefaultSensor(int type) {
        if(type>SensorWrapper.TYPE_CUSTOM_SENSOR_BASE) return getCustomSensor(type);

        Sensor asSensor = androidSensorManager.getDefaultSensor(type);
        if(asSensor!=null){
            return new AndroidSensorWrapper(this, asSensor);
        }
        return null;
    }
    public SensorWrapper getDefaultSensor(int type, boolean wakeUp) {
        if(type>SensorWrapper.TYPE_CUSTOM_SENSOR_BASE){
            SensorWrapper s = getCustomSensor(type, wakeUp);
            assert s!=null;
            //System.out.println(s.getType());
            return s;
        }
        else {
            return getAndroidSensor(type);
        }
    }
    private SensorWrapper getAndroidSensor(int type){
        assert type < SensorWrapper.TYPE_CUSTOM_SENSOR_BASE;
        if(allAndroidSensors[type]!=null){
            return allAndroidSensors[type];
        }
        allAndroidSensors[type] = new AndroidSensorWrapper(this, androidSensorManager.getDefaultSensor(type));
        return allAndroidSensors[type];
    }
    private @NonNull SensorWrapper getCustomSensor(int type){
        System.out.println("getCustomSensor "+type);

        assert idx(type)<allCustomSensors.length;
        assert type>SensorWrapper.TYPE_CUSTOM_SENSOR_BASE;

        if(allCustomSensors[idx(type)]!=null){
            return allCustomSensors[idx(type)];
        }
        switch (type){
            case SensorWrapper.TYPE_RACE_EVENT:
                allCustomSensors[idx(type)] = new RaceEventSensor();
                break;
            case SensorWrapper.TYPE_VELOCITY:
                allCustomSensors[idx(type)] = new VelocitySensor(this);
                break;
            case SensorWrapper.TYPE_POSITION:
                allCustomSensors[idx(type)] = new PositionSensor(this);
                break;
            case SensorWrapper.TYPE_COMPOSITE_ROTATION:
                allCustomSensors[idx(type)] = new CompositeRotationSensor(this);
                break;
        }
        // It's valid to ask for a number that doesn't exist, in which case we return null.
        return allCustomSensors[idx(type)];
    }
    private SensorWrapper getCustomSensor(int type, boolean wakeUp){
        throw new RuntimeException("Not implemented");
    }

    class AndroidSensorListener implements SensorEventListener{
        SensorEventListenerWrapper realListener;
        SensorManagerWrapper sensorManager;
        public AndroidSensorListener(SensorEventListenerWrapper listener, SensorManagerWrapper sensorManager){
            this.realListener = listener;
            this.sensorManager = sensorManager;
        }
        @Override
        public void onSensorChanged(SensorEvent androidEvent) {
            this.realListener.onSensorChanged(new AndroidSensorEventWrapper(androidEvent));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            this.realListener.onAccuracyChanged(getDefaultSensor(sensor.getType()), i);
        }
    }
    public void registerListener(SensorEventListenerWrapper listener, SensorWrapper sensor, int samplingPeriod){
        if(sensor.getType()<SensorWrapper.TYPE_CUSTOM_SENSOR_BASE){
            AndroidSensorWrapper asWrapper = (AndroidSensorWrapper)sensor;
            AndroidSensorListener asListener = new AndroidSensorListener(listener, SensorManagerWrapper.this);
            if(!androidSensorListeners.containsKey(listener)) androidSensorListeners.put(listener,new ArrayList<>());
            androidSensorListeners.get(listener).add(asListener);
            androidSensorManager.registerListener((SensorEventListener) asListener, asWrapper.secretGetAndroidSensor(), samplingPeriod);
        }
        else {
            sensor.registerListener(listener, samplingPeriod);
        }
    }
    public void unregisterListener(SensorEventListenerWrapper listener){
        for(SensorWrapper s: getSensorList(Sensor.TYPE_ALL)){
            s.unregisterListener(listener);
        }
        if(androidSensorListeners.containsKey(listener)){
            for(AndroidSensorListener asListener: androidSensorListeners.get(listener)){
                androidSensorManager.unregisterListener(asListener);
            }
            androidSensorListeners.remove(listener);
        }
    }

    public boolean flush(SensorEventListenerWrapper listener){
        boolean returnValue = true;
        if(this.androidSensorListeners.containsKey(listener)){
            for(AndroidSensorListener asListener: androidSensorListeners.get(listener)){
                boolean result = androidSensorManager.flush(asListener);
                returnValue = returnValue && result;
            }
        }
        else {
            for(SensorWrapper s: this.getCustomSensorList(Sensor.TYPE_ALL)){
                if(s.getType()<SensorWrapper.TYPE_CUSTOM_SENSOR_BASE) continue;
                if(s.listeners.contains(listener)){
                    boolean result = s.flush(listener);
                    returnValue = returnValue && result;
                }
            }
        }
        return returnValue;
    }
    public List<SensorWrapper> getSensorList(int type){
        List<Sensor> androidSensors = androidSensorManager.getSensorList(type);
        List<SensorWrapper> customSensors = getCustomSensorList(type);
        List<SensorWrapper> returnValues = new ArrayList<>(androidSensors.size()+SensorWrapper.TYPE_CUSTOM_SENSOR_MAX-SensorWrapper.TYPE_CUSTOM_SENSOR_BASE);
        for(Sensor as: androidSensors){
            returnValues.add(new AndroidSensorWrapper(this, as));
        }
        returnValues.addAll(customSensors);
        return returnValues;
    }
    public List<SensorWrapper> getCustomSensorList(int type){
        List<SensorWrapper> returnValues = new ArrayList<>(SensorWrapper.TYPE_CUSTOM_SENSOR_MAX-SensorWrapper.TYPE_CUSTOM_SENSOR_BASE);
        if(type>SensorWrapper.TYPE_CUSTOM_SENSOR_BASE){
            SensorWrapper s = getCustomSensor(type);
            if(s!=null) returnValues.add(s);
        }
        else if(type==Sensor.TYPE_ALL){
            for(int i=SensorWrapper.TYPE_CUSTOM_SENSOR_BASE+1; i<=SensorWrapper.TYPE_CUSTOM_SENSOR_MAX; i++){
                SensorWrapper s = getDefaultSensor(i);
                if(s!=null) returnValues.add(s);
            }
        }
        return returnValues;

    }
}

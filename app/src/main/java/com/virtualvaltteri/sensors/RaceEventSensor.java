package com.virtualvaltteri.sensors;

public class RaceEventSensor extends SensorWrapper {
    public RaceEventSensor(){
        super();
        TYPE = SensorWrapper.TYPE_RACE_EVENT;
        type = "RaceEvent";
    }
    public SensorEventWrapper triggerEvent(String subType){
        RaceEvent event = new RaceEvent(this, subType);
        genericEventHandler(event);
        return  event;
    }
    public SensorEventWrapper onEvent(String subType, String s){
        RaceEvent event = new RaceEvent(this, subType,s);
        genericEventHandler(event);
        return  event;
    }
    public SensorEventWrapper onEvent(String subType, String s, String s2){
        RaceEvent event = new RaceEvent(this, subType,s,s2);
        genericEventHandler(event);
        return  event;
    }
    public SensorEventWrapper onEvent(String subType, String s, String s2, String s3){
        RaceEvent event = new RaceEvent(this, subType,s,s2,s3);
        genericEventHandler(event);
        return  event;
    }
    public SensorEventWrapper onEvent(String subType, String s, String s2, String s3, String s4){
        RaceEvent event = new RaceEvent(this, subType,s,s2,s3,s4);
        genericEventHandler(event);
        return  event;
    }
}

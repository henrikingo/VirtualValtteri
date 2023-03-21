package com.virtualvaltteri.sensors;

public class GPSPositionEvent extends SensorEventWrapper{
    public GPSPositionEvent(double lat, double lon, long GPSTime){
        super();
        values = new float[3];
        values[0] = (float)lat;
        values[1] = (float) lon;
        values[2] = GPSTime;
    }
}

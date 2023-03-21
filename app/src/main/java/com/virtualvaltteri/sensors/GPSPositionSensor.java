package com.virtualvaltteri.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.concurrent.Executor;

public class GPSPositionSensor extends SensorWrapper implements LocationListener, Executor,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;    private final Context context;
    public long previousTimestamp;
    public FusedLocationProviderClient locationProvider;
    public GPSPositionSensor(SensorManagerWrapper sensorManager){
        super();
        TYPE = SensorWrapper.TYPE_GPS_POSITION;
        type = "GPS.Position";
        previousTimestamp = SystemClock.elapsedRealtimeNanos();
        this.context = sensorManager.context;
        locationProvider=LocationServices.getFusedLocationProviderClient(context);
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void registerListener(SensorEventListenerWrapper listener, int samplingPeriod){
        Task task;
        // API level 31
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            super.registerListener(listener, samplingPeriod);
            System.out.println("Android API Level 31 or higher found.");
            LocationRequest request = new LocationRequest.Builder(samplingPeriod/1000)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setWaitForAccurateLocation(true)
                    .build();
            task = locationProvider.requestLocationUpdates(request,(Executor) this,(LocationListener) this);
        }
        else{
            System.out.println("Android API Level 30 or lower found.");
            super.registerListener(listener, samplingPeriod);
            LocationRequest locationRequest = new LocationRequest().create()
                    .setInterval(1000)
                    .setFastestInterval(100);;
//            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//            locationRequest.setWaitForAccurateLocation(true);
            locationRequest.setInterval(1000);
            task = locationProvider.requestLocationUpdates(locationRequest,(Executor)this,(LocationListener)this);

            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLocation == null){
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                        mLocationRequest, this);
            }
            if (mLocation != null) {
                double latitude = mLocation.getLatitude();
                double longitude = mLocation.getLongitude();
            } else {
                // Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
            }
        }
        System.out.println(task.toString());
        System.out.println(task.isComplete());
//        System.out.println(task.getResult());
//        System.out.println(task.getException());
    }
    public void unregisterListener(SensorEventListenerWrapper listener){
        super.unregisterListener(listener);
        if(listeners.size()==0)
            locationProvider.removeLocationUpdates(this);
    }
    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        System.out.println("GPS location changed");
        final double lat = location.getLatitude();
        final double lon = location.getLongitude();
        final long GPSTime = location.getTime();
        GPSPositionEvent event = new GPSPositionEvent(lat,lon,GPSTime);
        if (event.timestamp>previousTimestamp+samplingPeriod){
            for(SensorEventListenerWrapper listener: listeners){
                listener.onSensorChanged(event);
            }

            previousTimestamp=event.timestamp;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        System.out.println("Connection created");
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        System.out.println("Connection failed. Error: " + connectionResult.getErrorCode());
    }

    public void onStart() {
        mGoogleApiClient.connect();
    }

    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

}

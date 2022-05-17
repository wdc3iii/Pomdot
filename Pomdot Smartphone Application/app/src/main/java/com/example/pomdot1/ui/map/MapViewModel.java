package com.example.pomdot1.ui.map;

import android.location.Location;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import java.time.LocalDateTime;

public class MapViewModel extends ViewModel {
    private static final String TAG = MapViewModel.class.getSimpleName();

    private MutableLiveData<Location> currSelfLocation;
    private MutableLiveData<LatLng> currDeviceLocation;
    private MutableLiveData<LocalDateTime> deviceTimeLastLocation;
    private float currDeviceLocationLatitude;
    private float currDeviceLocationLongitude;

    public MapViewModel() {
        currSelfLocation = new MutableLiveData<>();
        currDeviceLocation = new MutableLiveData<>();
    }

    public MutableLiveData<Location> getCurrSelfLocation() {
        if (currSelfLocation == null) {
            currSelfLocation = new MutableLiveData<>();
        }
        return currSelfLocation;
    }
    public MutableLiveData<LatLng> getCurrDeviceLocation() {
        if (currDeviceLocation == null) {
            currDeviceLocation = new MutableLiveData<>();
        }
        return currDeviceLocation;
    }

    public void setCurrSelfLocation(Location location) {
        currSelfLocation.setValue(location);
    }

    public void setCurrDeviceLocationLatitude(float latitude){
        Log.i("LATITUTDE", Float.toString(latitude));
        if (latitude != -1000) {
            currDeviceLocationLatitude = latitude;
            updateDeviceLocation();
        }
    }
    public void setCurrDeviceLocationLongitude(float longitude){
        Log.i("LONGITUDE", Float.toString(longitude));
        if (longitude != -1000) {
            currDeviceLocationLongitude = longitude;
            updateDeviceLocation();
        }
    }

    private void updateDeviceLocation() {
        currDeviceLocation.setValue(new LatLng(currDeviceLocationLatitude, currDeviceLocationLongitude));
    }
}
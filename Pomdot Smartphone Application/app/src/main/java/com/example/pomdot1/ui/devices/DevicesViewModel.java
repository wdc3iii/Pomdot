package com.example.pomdot1.ui.devices;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class DevicesViewModel extends ViewModel {

    private static final String NO_DEVICES_CONNECTED = "No Devices Connected";
    private static final String NONE = "";
    private final MutableLiveData<String> deviceAddress;
    private final MutableLiveData<String> deviceName;
    private final MutableLiveData<Integer> deviceDetach;
    private final MutableLiveData<Boolean> deviceConnectedBluetooth;
    private final MutableLiveData<LocalDateTime> deviceTimeLastLocation;
    private int deviceTimeLastLocationSecond;
    private int deviceTimeLastLocationMinute;
    private int deviceTimeLastLocationHour;
    private int deviceTimeLastLocationDay;
    private int deviceTimeLastLocationMonth;
    private int deviceTimeLastLocationYear;

    public DevicesViewModel() {
        deviceAddress = new MutableLiveData<>();
        deviceName = new MutableLiveData<>();
        deviceDetach = new MutableLiveData<>();
        deviceConnectedBluetooth = new MutableLiveData<>();
        deviceTimeLastLocation = new MutableLiveData<>();
    }

    public void clearUI() {
        deviceName.setValue(NO_DEVICES_CONNECTED);
        deviceDetach.setValue(null);
        deviceConnectedBluetooth.setValue(false);
        deviceTimeLastLocation.setValue(null);
    }

    public void setDeviceTimeLastLocationSecond(int second) {
        if (second != -1) {
            deviceTimeLastLocationSecond = second;
            updateTimeLastLocation();
        }
    }
    public void setDeviceTimeLastLocationMinute(int minute) {
        if (minute != -1) {
            deviceTimeLastLocationMinute = minute;
            updateTimeLastLocation();
        }
    }
    public void setDeviceTimeLastLocationHour(int hour) {
        if (hour != -1) {
            deviceTimeLastLocationHour = hour;
            updateTimeLastLocation();
        }
    }
    public void setDeviceTimeLastLocationDay(int day) {
        if (day != -1) {
            deviceTimeLastLocationDay = day;
            updateTimeLastLocation();
        }
    }
    public void setDeviceTimeLastLocationMonth(int month) {
        if (month != -1) {
            deviceTimeLastLocationMonth = month;
            updateTimeLastLocation();
        }
    }
    public void setDeviceTimeLastLocationYear(int year) {
        if (year != -1) {
            deviceTimeLastLocationYear = year;
            updateTimeLastLocation();
        }
    }

    private int bytesToInt(byte[] value) {
        int intValue = 0;
        for (int ii = value.length - 1; ii >= 0; ii--) {
            intValue = (intValue << 8) + (value[ii] & 0xFF);
        }
        return intValue;
    }
    private float bytesToFloat(byte[] value) {
        return Float.intBitsToFloat(bytesToInt(value));
    }
    private String bytesToString(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    public void setDeviceTimeLastLocation(int time) {
        int seconds = time % 100;
        time /= 100;
        int minutes = time % 100;
        time /= 100;
        int hours = time;
        setDeviceTimeLastLocationSecond(seconds);
        setDeviceTimeLastLocationMinute(minutes);
        setDeviceTimeLastLocationHour(hours);
    }

    public void setDeviceDateLastLocation(int date) {
        int detachVal = date % 10;
        date /= 10;
        int days = date % 100;
        date /= 100;
        int months = date % 100;
        date /= 100;
        int years = date + 2000;
        setDeviceTimeLastLocationDay(days);
        setDeviceTimeLastLocationMonth(months);
        setDeviceTimeLastLocationYear(years);
        setDeviceDetach(detachVal);
    }

    public void setDeviceAddress(String address) {
        deviceAddress.setValue(address);
    }
    public void setDeviceName(String name) {
        deviceName.setValue(name);
    }
    public void setDeviceDetach(int detach) {
        deviceDetach.setValue(detach);
    }
    public void setDeviceConnectedBluetooth(boolean connected) {
        deviceConnectedBluetooth.setValue(connected);
    }

    private void updateTimeLastLocation() {
        if (deviceTimeLastLocationYear > 0 &&
                deviceTimeLastLocationMonth >= 1 && deviceTimeLastLocationMonth <= 12 &&
                deviceTimeLastLocationDay >= 1 && deviceTimeLastLocationDay <= 31 &&
                deviceTimeLastLocationHour >= 0 && deviceTimeLastLocationHour < 24 &&
                deviceTimeLastLocationMinute >= 0 && deviceTimeLastLocationMinute < 60 &&
                deviceTimeLastLocationSecond >= 0 && deviceTimeLastLocationSecond < 60) {
            deviceTimeLastLocation.setValue(LocalDateTime.of(
                    deviceTimeLastLocationYear,
                    deviceTimeLastLocationMonth,
                    deviceTimeLastLocationDay,
                    deviceTimeLastLocationHour,
                    deviceTimeLastLocationMinute,
                    deviceTimeLastLocationSecond
            ));
        }
    }

    public LiveData<String> getDeviceAddress() {
        return deviceAddress;
    }
    public LiveData<String> getDeviceName() {
        return deviceName;
    }
    public LiveData<Integer> getDeviceDetach() {
        return deviceDetach;
    }
    public LiveData<Boolean> getDeviceConnectedBluetooth() {
        return deviceConnectedBluetooth;
    }
    public LiveData<LocalDateTime> getDeviceTimeLastLocation() {
        return deviceTimeLastLocation;
    }
}
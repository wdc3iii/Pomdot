package com.example.pomdot1;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class BLEService extends Service {
    private final static String TAG = BLEService.class.getSimpleName();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String LATITUDE = "com.example.bluetooth.le.LATITUDE";
    public final static String LONGITUDE = "com.example.bluetooth.le.LONGITUDE";
    public final static String DATE = "com.example.bluetooth.le.DATE";
    public final static String TIME = "com.example.bluetooth.le.TIME";
    public final static String NAME = "com.example.bluetooth.le.NAME";
    public final static String DETACHED = "com.example.bluetooth.le.DETACHED";

    private boolean requestingBLEPermissions;

    public final static UUID UUID_LOCATION_LAT = UUID.fromString(SampleGattAttributes.LOCATION_LAT_CHAR_UUID);
    public final static UUID UUID_LOCATION_LAT_DESC = UUID.fromString(SampleGattAttributes.LOCATION_LAT_DESC_UUID);
    public final static UUID UUID_LOCATION_LON = UUID.fromString(SampleGattAttributes.LOCATION_LON_CHAR_UUID);
    public final static UUID UUID_LOCATION_LON_DESC = UUID.fromString(SampleGattAttributes.LOCATION_LON_DESC_UUID);
    public final static UUID UUID_TIME = UUID.fromString(SampleGattAttributes.TIME_CHAR_UUID);
    public final static UUID UUID_TIME_DESC = UUID.fromString(SampleGattAttributes.TIME_DESC_UUID);
    public final static UUID UUID_DATE = UUID.fromString(SampleGattAttributes.DATE_CHAR_UUID);
    public final static UUID UUID_DATE_DESC = UUID.fromString(SampleGattAttributes.DATE_DESC_UUID);
    public final static UUID UUID_NAME = UUID.fromString(SampleGattAttributes.POMDOT_CHAR_UUID);
    public final static UUID UUID_NAME_DESC = UUID.fromString(SampleGattAttributes.POMDOT_DESC_UUID);
    public final static UUID UUID_DETACHED = UUID.fromString(SampleGattAttributes.DETACHED_CHAR_UUID);
    public final static UUID UUID_DETACHED_DESC = UUID.fromString(SampleGattAttributes.DETACHED_DESC_UUID);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt.discoverServices();//, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth Permissions Error", Toast.LENGTH_SHORT).show();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
//                Toast.makeText(getApplicationContext(), "Disconnected from GATT Server", Toast.LENGTH_SHORT).show();
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Toast.makeText(getApplicationContext(), "onServicesDiscovered received" + status, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.i("CHARACTERISTIC_READ", "Read " + characteristic.getUuid().toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i("CHARACTERISTIC_CHANGED", "Changed " + characteristic.getUuid().toString());
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
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

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        byte[] value = characteristic.getValue();
        if (UUID_LOCATION_LAT.equals(characteristic.getUuid())) {
            intent.putExtra(LATITUDE, bytesToFloat(value));
        } else if (UUID_LOCATION_LON.equals(characteristic.getUuid())) {
            intent.putExtra(LONGITUDE, bytesToFloat(value));
        } else if (UUID_TIME.equals(characteristic.getUuid())) {
            int time = bytesToInt(value);
            intent.putExtra(TIME, time);
        } else if (UUID_DATE.equals(characteristic.getUuid())) {
            int date = bytesToInt(value);
            intent.putExtra(DATE, date);
        } else if (UUID_DETACHED.equals(characteristic.getUuid())) {
            intent.putExtra(DETACHED, bytesToInt(value));
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final IBinder mBinder = new LocalBinder();
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }



    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Toast.makeText(getApplicationContext(), "Unable to initialize Bluetooth Manager.", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Unable to obtain a BluetoothAdapter.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (bluetoothAdapter == null || address == null) {
            Toast.makeText(getApplicationContext(), "BluetoothAdapter not initialized or unspecified address. Cannot connect", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (address.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Toast.makeText(getApplicationContext(), "Trying to use an existing mBluetoothGatt for connection.", Toast.LENGTH_SHORT).show();
            if (bluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Toast.makeText(getApplicationContext(), "Device not found.  Unable to connect.", Toast.LENGTH_SHORT).show();
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = device.connectGatt(this, true, gattCallback);
//        Toast.makeText(getApplicationContext(), "Trying to create a new connection.", Toast.LENGTH_SHORT).show();
        bluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt = null;
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Toast.makeText(getApplicationContext(), "BluetoothAdapter not initialized, cannot read characteristic", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Toast.makeText(getApplicationContext(), "BluetoothAdapter not initialized, cannot set characteristic notification", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
//        Toast.makeText(this, characteristic.toString(), Toast.LENGTH_SHORT).show();
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (UUID_LOCATION_LAT.equals(characteristic.getUuid())){
            BluetoothGattDescriptor descriptor =
                    new BluetoothGattDescriptor(UUID_LOCATION_LAT_DESC, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        } else if (UUID_LOCATION_LON.equals(characteristic.getUuid())){
            BluetoothGattDescriptor descriptor =
                    new BluetoothGattDescriptor(UUID_LOCATION_LON_DESC, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        } else if (UUID_TIME.equals(characteristic.getUuid())){
            BluetoothGattDescriptor descriptor =
                    new BluetoothGattDescriptor(UUID_TIME_DESC, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        } else if (UUID_DATE.equals(characteristic.getUuid())){
            BluetoothGattDescriptor descriptor =
                    new BluetoothGattDescriptor(UUID_DATE_DESC, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        } else if (UUID_DETACHED.equals(characteristic.getUuid())){
            BluetoothGattDescriptor descriptor =
                    new BluetoothGattDescriptor(UUID_DETACHED_DESC, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        } else if (UUID_NAME.equals(characteristic.getUuid())){
            BluetoothGattDescriptor descriptor =
                    new BluetoothGattDescriptor(UUID_NAME_DESC, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
    }
}
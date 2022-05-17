package com.example.pomdot1;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.pomdot1.ui.devices.DevicesViewModel;
import com.example.pomdot1.ui.map.MapViewModel;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.pomdot1.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private MapViewModel mapViewModel;
    private DevicesViewModel devicesViewModel;
    private CancellationTokenSource cancellationSource;
    private BLEService bleService;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private String deviceAddress;
    private WorkManager workManager;
    private Handler handler;
    private boolean locationPermissions;
    private boolean bluetoothPermissions;
    private boolean networkPermissions;
    private final int latInd = 1;
    private final int lonInd = 2;
    private final int timeInd = 3;
    private final int dateInd = 4;
    private final int removalDetection = 5;
    private int detachCount;
    private final int CELLULAR_UPDATE_RATE = 1000;
    private static final int REQUEST_CHECK_SETTINGS = 1;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final String API_KEY = "EQ8C2VOY6MVSFANS";
    private final String urlFormat = "https://api.thingspeak.com/channels/1694933/fields/%d.json?api_key=%s&results=2";
    private final String WORK_TAG = "com.example.serverexample.CellularWorkTag";

    private final LocationCallback locationCallback= new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            mapViewModel.setCurrSelfLocation(locationResult.getLastLocation());
        }
    };

    // Keeps track of the results of the permission requests.
    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                locationPermissions = isGranted;
            });
    private final ActivityResultLauncher<String> requestBluetoothPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                bluetoothPermissions = isGranted;
            });
    private final ActivityResultLauncher<String> requestNetworkPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                networkPermissions = isGranted;
            });

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BLEService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.i(TAG,"Unable to initialize bluetooth.");
                Toast.makeText(getApplicationContext(), "Failed to establish bluetooth service.", Toast.LENGTH_SHORT).show();
            } else {
                // Automatically connects to the device upon successful start-up initialization.
                if (deviceAddress != null) {
                    bleService.connect(deviceAddress);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
//            Toast.makeText(getApplicationContext(), "Recieving Broadcast " + action.toString(), Toast.LENGTH_SHORT).show();
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                devicesViewModel.setDeviceConnectedBluetooth(true);
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                devicesViewModel.setDeviceConnectedBluetooth(false);
            } else if (BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.

                enableGattNotifications(bleService.getSupportedGattServices());
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (intent.hasExtra(BLEService.DETACHED)) {
                    devicesViewModel.setDeviceDetach(intent.getIntExtra(BLEService.DETACHED, -1));
                }
                if (intent.hasExtra(BLEService.LATITUDE)) {
                    mapViewModel.setCurrDeviceLocationLatitude(
                            intent.getFloatExtra(BLEService.LATITUDE, -1000)
                    );
                }
                if (intent.hasExtra(BLEService.LONGITUDE)) {
                    mapViewModel.setCurrDeviceLocationLongitude(
                            intent.getFloatExtra(BLEService.LONGITUDE, -1000)
                    );
                }
                if (intent.hasExtra(BLEService.TIME)) {
                    devicesViewModel.setDeviceTimeLastLocation(
                            intent.getIntExtra(BLEService.TIME, -1)
                    );
                }
                if (intent.hasExtra(BLEService.DATE)) {
                    devicesViewModel.setDeviceDateLastLocation(
                            intent.getIntExtra(BLEService.DATE, -1)
                    );
                }
            }
        }
    };

    private void enableGattNotifications(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().
                getString(R.string.unknown_service);
        String unknownCharaString = getResources().
                getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData =
                new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics =
                new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData =
                    new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.
                            lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattCharacteristics) {
                bleService.setCharacteristicNotification(gattCharacteristic, true);
                bleService.readCharacteristic(gattCharacteristic);
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData =
                        new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid,
                                unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);
        devicesViewModel = new ViewModelProvider(this).get(DevicesViewModel.class);
        detachCount = 0;
        workManager = WorkManager.getInstance(this);
        handler = new Handler(Looper.getMainLooper());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_map, R.id.navigation_devices, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        // Handle Permissions
        if (!checkLocationPermissions()) {
            Log.i(TAG, "Location Permissions Check Failed: launching permission request.");
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            Log.i(TAG, "Location Permissions Check Passed.");
        }
        if (!checkBluetoothLEPermissions()) {
            Log.i(TAG, "Location Permissions Check Failed: launching permission request.");
            requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH);
        } else {
            Log.i(TAG, "Location Permissions Check Passed.");
        }
        if (!checkNetworkPermssions()) {
            Log.i(TAG, "Network Permissions Check Failed: launching permission request.");
            requestBluetoothPermissionLauncher.launch(Manifest.permission.INTERNET);
        } else {
            Log.i(TAG, "Network Permissions Check Passed.");
        }

        // Create a location request, for real-time update of phone location on map
        cancellationSource = new CancellationTokenSource();
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Check to see if current location settings are sufficient
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addAllLocationRequests(Collections.singleton(locationRequest));
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(
                                MainActivity.this, REQUEST_CHECK_SETTINGS);
                        Log.i(TAG, "Location Settings check task failed, suggesting fix.");
                    } catch (IntentSender.SendIntentException sendEx) {
                        Log.i(TAG, "Location Settings check task failed.");
                    }
                }
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(
                this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            mapViewModel.setCurrSelfLocation(location);
                        }
                    }
                });

        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        final Observer<String> deviceAddressObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String address) {
                if (address != null && bleService != null) {
                    deviceAddress = address;
                    bleService.connect(address);
                    List<BluetoothGattService> serviceList = bleService.getSupportedGattServices();
                }
            }
        };
        devicesViewModel.getDeviceAddress().observe(this, deviceAddressObserver);
        final Observer<Integer> deviceDetachObserver = new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable final Integer detachValue) {
                Log.i("DETACH", "Incoming: " + Integer.toString(detachValue) + ", Current: " + Integer.toString(detachCount));
                if (detachValue == detachCount) {
                    return;
                }

                detachCount = detachValue;
                // Create the object of the AlertDialog Builder class
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                // Set the message show for the Alert time
                builder.setMessage("UNAUTHORIZED DEVICE REMOVAL");
                // Set Alert Title
                builder.setTitle("WARNING!");

                // Set Cancelable false so that it remains on screen if user clicks off
                builder.setCancelable(false);

                // Set the positive button
                builder.setPositiveButton("Warning Received", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                // Create the Alert dialog
                AlertDialog alertDialog = builder.create();
                // Show the Alert Dialog box
                alertDialog.show();
            }
        };
        devicesViewModel.getDeviceDetach().observe(this, deviceDetachObserver);
        final Observer<Boolean> deviceConnectedBluetoothObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable final Boolean connected) {
                if (connected) {
                    // Stop cellular updates
                    stopCellularUpdates();
                } else {
                    // Begin cellular updates
                    startCellularUpdates();
                }
            }
        };
        devicesViewModel.getDeviceAddress().observe(this, deviceAddressObserver);
        devicesViewModel.getDeviceConnectedBluetooth().observe(this, deviceConnectedBluetoothObserver);
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller,
                                             @NonNull NavDestination destination, @Nullable Bundle arguments) {
                if (destination.getId() == R.id.navigation_map) {
                    cancellationSource = new CancellationTokenSource();
                    startLocationUpdates();
                } else {
                    if (cancellationSource != null) {
                        cancellationSource.cancel();
                    }
                    stopLocationUpdates();
                }
            }
        });
    }

    public void startLocationUpdates() {
        if (checkLocationPermissions()) {
            Log.i(TAG, "Starting location updates: permissions found.");
            fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Log.i(TAG, "Location updates failed: permissions NOT found.");
        }

    }

    public void stopLocationUpdates() {
        Log.i(TAG, "Stopping Location Updates.");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    public void startCellularUpdates() {
        handler.postDelayed(updateDeviceStats, CELLULAR_UPDATE_RATE);
    }

    public void stopCellularUpdates() {
        handler.removeCallbacks(updateDeviceStats);
        workManager.cancelAllWorkByTag(WORK_TAG);
    }
    private final Runnable updateDeviceStats = new Runnable() {
        @Override
        public void run() {
            for (int ind = 1; ind <= 4; ind++) {
                WorkRequest workRequest = new OneTimeWorkRequest.Builder(CellularWorker.class).setInputData(
                        new Data.Builder()
                                .putString(CellularWorker.URL_FORMAT_ARG, urlFormat)
                                .putString(CellularWorker.API_KEY_ARG, API_KEY)
                                .putInt(CellularWorker.FIELD_INDEX_ARG, ind)
                                .build()
                ).addTag(WORK_TAG).build();
                workManager.enqueue(workRequest);
                WorkManager.getInstance(MainActivity.this).getWorkInfoByIdLiveData(workRequest.getId())
                        .observe(MainActivity.this, result -> {
                            if (result != null && result.getState().isFinished()) {
                                int index = result.getOutputData().getInt(CellularWorker.INDEX_ARG, -1);
                                if (index <= 2) {
                                    float value = result.getOutputData().getFloat(CellularWorker.RESULT_ARG, -1);
                                    if (index == latInd) {
                                        Log.i("GPS LAT", Float.toString(value));
                                        mapViewModel.setCurrDeviceLocationLatitude(value);
                                    } else if (index == lonInd) {
                                        Log.i("GPS LON", Float.toString(value));
                                        mapViewModel.setCurrDeviceLocationLongitude(value);
                                    }
                                } else {
                                    int value = result.getOutputData().getInt(CellularWorker.RESULT_ARG, -1);
                                    if (index == timeInd) {
                                        Log.i("GPS TIME", Integer.toString(value));
                                        devicesViewModel.setDeviceTimeLastLocation(value);
                                    } else if (index == dateInd) {
                                        Log.i("GPS DATE", Integer.toString(value));
                                        devicesViewModel.setDeviceDateLastLocation(value);
                                    }
//                                    else if (index == removalDetection) {
//                                        devicesViewModel.setDeviceDetach(value);
//                                    }
                                }
                            }
                        });
            }
            handler.postDelayed(this, CELLULAR_UPDATE_RATE);
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private boolean checkFineLocationPermission() {
        return ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
    private boolean checkCoarseLocationPermission() {
        return ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
    private boolean checkLocationPermissions() {
        return checkCoarseLocationPermission() && checkFineLocationPermission();
    }
    private boolean checkBluetoothConnectPermission() {
        return ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }
    private boolean checkBluetoothScanPermission() {
        return ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED;
    }
    private boolean checkBluetoothLEPermissions() {
        return checkBluetoothConnectPermission() && checkBluetoothScanPermission();
    }
    private boolean checkNetworkPermssions() {
        return ActivityCompat.checkSelfPermission(
                this, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bleService != null) {
            final boolean result = bleService.connect(deviceAddress);
        }
        // TODO: Restart location updates?
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cancellationSource != null){
            cancellationSource.cancel();
        }
        stopLocationUpdates();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cancellationSource != null){
            cancellationSource.cancel();
        }
        stopLocationUpdates();
        unbindService(serviceConnection);
        bleService = null;
    }
}
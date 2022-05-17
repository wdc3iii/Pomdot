package com.example.pomdot1.ui.devicescan;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pomdot1.R;
import com.example.pomdot1.SampleGattAttributes;
import com.example.pomdot1.ui.devices.DevicesFragment;
import com.example.pomdot1.ui.devices.DevicesViewModel;
import com.example.pomdot1.ui.deviceslist.DevicesListFragment;

import java.util.ArrayList;
import java.util.Collections;

public class DeviceScanFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = DevicesFragment.class.getSimpleName();
    private DevicesViewModel devicesViewModel;
    private Button btnCancel;
    private Button btnRescan;
    private DeviceScanFragment.BleScanDeviceListAdapter bleScanDeviceListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private boolean mScanning;
    private Handler mHandler;
    private RecyclerView availableDevicesRecView;
    private static final ScanFilter scanFilter = new ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(SampleGattAttributes.POMDOT_SERV_UUID)).build();
    private static final ScanSettings scanSettings = new ScanSettings.Builder().build();
    private static final long SCAN_PERIOD = 5000;

    private boolean requestingBLEPermissions;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                requestingBLEPermissions = isGranted;
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_device_scan, container, false);
        devicesViewModel = new ViewModelProvider(requireActivity()).get(DevicesViewModel.class);
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
        }

        mHandler = new Handler(Looper.getMainLooper());
        availableDevicesRecView = root.findViewById(R.id.devicescan_rec_view);
        btnRescan = root.findViewById(R.id.btn_rescan);
        btnRescan.setOnClickListener(this);
        btnCancel = root.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(this);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(requireActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(requireActivity(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        }
        // Initializes list view adapter.
        bleScanDeviceListAdapter = new DeviceScanFragment.BleScanDeviceListAdapter();
        availableDevicesRecView.setAdapter(bleScanDeviceListAdapter);
        availableDevicesRecView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        scanLeDevice(true);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        scanLeDevice(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel:
                scanLeDevice(false);
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.setReorderingAllowed(true);

                // Replace whatever is in the fragment_container view with this fragment
                transaction.replace(R.id.fragment_devices_container, DevicesListFragment.class, null);
                transaction.commit();
                break;
            case R.id.btn_rescan:
                bleScanDeviceListAdapter.clear();
                btnRescan.setEnabled(false);
                btnRescan.setText(R.string.scanning);
                scanLeDevice(true);
                break;
            default:
                break;
        }
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            btnRescan.setEnabled(false);
            btnRescan.setText(R.string.scanning);
            // Stops scanning after a pre-defined scan period.
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
            }
            mScanning = true;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    try {
                        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
                        }
                        bleScanner.stopScan(bleScanCallback);
                        btnRescan.setEnabled(true);
                        btnRescan.setText(R.string.rescan);
                    } catch (IllegalStateException e) {
                        Log.i(TAG, "postDelayed BLE Scan errored, fragment no longer exists" + e.getMessage());
                    }

                }
            }, SCAN_PERIOD);
            bleScanner.startScan(Collections.singletonList(scanFilter), scanSettings, bleScanCallback);
        } else {
            mScanning = false;
            bleScanDeviceListAdapter.clear();
            bleScanner.stopScan(bleScanCallback);
        }
    }

    private class BleScanDeviceListAdapter extends RecyclerView.Adapter<DeviceScanFragment.BleScanDeviceListAdapter.ViewHolder> {
        private ArrayList<BluetoothDevice> bleDevices;
        private final LayoutInflater inflater;

        public class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView txtDeviceName;
            private final TextView txtDeviceAddress;
            private final LinearLayout parent;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtDeviceAddress = itemView.findViewById(R.id.device_address);
                txtDeviceName = itemView.findViewById(R.id.device_name);
                parent = itemView.findViewById(R.id.device_scan_lin_layout);
            }
        }

        @NonNull
        @Override
        public DeviceScanFragment.BleScanDeviceListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_available_devices, parent, false);
            return new DeviceScanFragment.BleScanDeviceListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceScanFragment.BleScanDeviceListAdapter.ViewHolder holder, int position) {
            BluetoothDevice device = bleDevices.get(holder.getAdapterPosition());
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
            }
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                holder.txtDeviceName.setText(deviceName);
            else
                holder.txtDeviceName.setText(R.string.unknown_device);
            holder.txtDeviceAddress.setText(device.getAddress());

            holder.parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final BluetoothDevice device = bleScanDeviceListAdapter.getDevice(holder.getAdapterPosition());
                    if (device == null) return;

                    if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                    }

                    devicesViewModel.setDeviceName(device.getName());
                    devicesViewModel.setDeviceAddress(device.getAddress());
                    scanLeDevice(false);
                    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.setReorderingAllowed(true);

                    // Replace whatever is in the fragment_container view with this fragment
                    transaction.replace(R.id.fragment_devices_container, DevicesListFragment.class, null);
                    transaction.commit();
                }
            });

        }

        @Override
        public int getItemCount() {
            return bleDevices.size();
        }

        public BleScanDeviceListAdapter() {
            super();
            bleDevices = new ArrayList<>();
            inflater = DeviceScanFragment.this.getLayoutInflater();
        }

        public void setDevices(ArrayList<BluetoothDevice> devices) {
            bleDevices = devices;
            notifyDataSetChanged();
        }

        public void addDevice(BluetoothDevice device) {
            if (!bleDevices.contains(device)) {
                bleDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return bleDevices.get(position);
        }

        public void clear() {
            bleDevices.clear();
        }

        public Object getItem(int i) {
            return bleDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
    }

    // Device scan callback.
    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            bleScanDeviceListAdapter.addDevice(result.getDevice());
            bleScanDeviceListAdapter.notifyDataSetChanged();
        }
    };
}
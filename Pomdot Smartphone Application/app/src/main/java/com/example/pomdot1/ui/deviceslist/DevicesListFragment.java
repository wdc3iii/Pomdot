package com.example.pomdot1.ui.deviceslist;

import com.example.pomdot1.R;
import com.example.pomdot1.databinding.FragmentDevicesListBinding;
import com.example.pomdot1.ui.devices.DevicesViewModel;
import com.example.pomdot1.ui.devicescan.DeviceScanFragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DevicesListFragment extends Fragment implements View.OnClickListener {

    private final static String TAG = DevicesListFragment.class.getSimpleName();

    private FragmentDevicesListBinding binding;
    private TextView txtDeviceName;
    private TextView txtDeviceBattery;
    private TextView txtDeviceConnected;
    private TextView txtDeviceTimeLastLocation;
    private LocalDateTime deviceTimeLastLocation;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDevicesListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        DevicesViewModel devicesViewModel = new ViewModelProvider(requireActivity()).get(DevicesViewModel.class);

        // Sets up UI references.
        Button btnScan = root.findViewById(R.id.btn_connect);
        btnScan.setOnClickListener(this);
        txtDeviceName = root.findViewById(R.id.device_name);
        txtDeviceBattery = root.findViewById(R.id.device_battery);
        txtDeviceConnected = root.findViewById(R.id.device_connected);
        txtDeviceTimeLastLocation = root.findViewById(R.id.device_time_last_location);

        // Create the observer which updates the UI.
        final Observer<String> deviceNameObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String name) {
                // Update the UI, in this case, a TextView.
                txtDeviceName.setText(name);
            }
        };
        final Observer<Float> deviceBatteryObserver = new Observer<Float>() {
            @Override
            public void onChanged(@Nullable final Float battery) {
                // Update the UI, in this case, a TextView.
                txtDeviceBattery.setText(String.format("Battery Remaining: %d%%", battery));
            }
        };
        final Observer<Boolean> deviceConnectedObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable final Boolean connected) {
                // Update the UI, in this case, a TextView.
                if (connected != null) {
                    txtDeviceConnected.setText(connected ? "Connected via Bluetooth" : "Bluetooth Disconnected");
                }

            }
        };
        final Observer<LocalDateTime> deviceTimeLastLocationObserver = new Observer<LocalDateTime>() {
            @Override
            public void onChanged(@Nullable final LocalDateTime dateTime) {
                // Update the UI, in this case, a TextView.
                if (dateTime == null) {
                    txtDeviceTimeLastLocation.setText(R.string.no_location_results);
                } else {
                    deviceTimeLastLocation = dateTime;
                    updateDeviceTimeLastLocation();
                }
            }
        };
        devicesViewModel.getDeviceConnectedBluetooth().observe(getViewLifecycleOwner(), deviceConnectedObserver);
        devicesViewModel.getDeviceName().observe(getViewLifecycleOwner(), deviceNameObserver);
//        devicesViewModel.getDeviceTimeLastLocation().observe(getViewLifecycleOwner(), deviceTimeLastLocationObserver);

//        final Handler handler = new Handler(Looper.getMainLooper());
//        final int delay = 5000;
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                updateDeviceTimeLastLocation();
//            }
//        }, delay);

        return root;
    }

    public void updateDeviceTimeLastLocation() {
        if (deviceTimeLastLocation != null) {
            Duration elapsedTime = Duration.between(deviceTimeLastLocation, LocalDateTime.now(ZoneOffset.UTC));
            String elapsedStr;
            if (elapsedTime.toDays() > 365) {
                elapsedStr = String.format("%.1f years", elapsedTime.toDays() / 365.0);
            } else if (elapsedTime.toDays() > 31) {
                elapsedStr = String.format("%.1f months", elapsedTime.toDays() / 31.0);
            } else if (elapsedTime.toHours() > 24) {
                elapsedStr = String.format("%.1f days", elapsedTime.toHours() / 24.0);
            } else if (elapsedTime.toMinutes() > 60) {
                elapsedStr = String.format("%.1f hours", elapsedTime.toMinutes() / 60.0);
            } else if (elapsedTime.toMillis() > 60000) {
                elapsedStr = String.format("%.1f minutes", elapsedTime.toMillis() / 60000.0);
            } else {
                elapsedStr = String.format("%d seconds", elapsedTime.toMillis() / 1000);
            }
            txtDeviceTimeLastLocation.setText(getString(R.string.time_since_last_location_update) + elapsedStr);
        }

    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_connect:
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.setReorderingAllowed(true);

                // Replace whatever is in the fragment_container view with this fragment
                transaction.replace(R.id.fragment_devices_container, DeviceScanFragment.class, null);
                transaction.commitNow();
                break;
            default:
                break;
        }
    }
}
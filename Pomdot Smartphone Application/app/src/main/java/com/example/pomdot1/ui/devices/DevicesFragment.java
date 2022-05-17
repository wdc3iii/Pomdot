package com.example.pomdot1.ui.devices;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.pomdot1.R;
import com.example.pomdot1.databinding.FragmentDevicesBinding;
import com.example.pomdot1.ui.deviceslist.DevicesListFragment;

public class DevicesFragment extends Fragment {

    private final static String TAG = DevicesFragment.class.getSimpleName();

    private FragmentDevicesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        DevicesViewModel devicesViewModel = new ViewModelProvider(this).get(DevicesViewModel.class);

        binding = FragmentDevicesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_devices_container, DevicesListFragment.class, null)
                .setReorderingAllowed(true)
                .commit();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
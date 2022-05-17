package com.example.pomdot1.ui.map;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.pomdot1.R;
import com.example.pomdot1.databinding.FragmentMapBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapBinding binding;
    private GoogleMap mMap;
    private MapViewModel mapViewModel;
    private Location currSelfLocation;
    private Marker currSelfLocationMarker;
    private LatLng currDeviceLocation;
    private Marker currDeviceLocationMarker;
    private boolean centerMap;
    private static final String TAG = MapFragment.class.getSimpleName();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        centerMap = true;
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);


        // Create the observer which updates the UI.
        final Observer<Location> selfLocationObserver = newLocation -> {
            // Update the UI, in this case, a TextView.
            currSelfLocation = newLocation;
            updateUI();
        };
        final Observer<LatLng> deviceLocationObserver = newLatLng -> {
            // Update the UI, in this case, a TextView.
            currDeviceLocation = newLatLng;
            updateUI();
        };

        mapViewModel.getCurrSelfLocation().observe(getViewLifecycleOwner(), selfLocationObserver);
        mapViewModel.getCurrDeviceLocation().observe(getViewLifecycleOwner(), deviceLocationObserver);

        updateUI();
        return root;
    }

    private void updateUI() {
        if (mMap == null) {
            return;
        }
        if (currSelfLocationMarker != null) {
            Log.i(TAG, "UpdateUI: current self location marker deleted");
            currSelfLocationMarker.remove();
        }
        if (currDeviceLocationMarker != null) {
            Log.i(TAG, "UpdateUI: current self location marker deleted");
            currDeviceLocationMarker.remove();
        }
        if (currSelfLocation != null) {
            Log.i(TAG, "UpdateUI: Add self marker to the map.");
            LatLng latLng = new LatLng(currSelfLocation.getLatitude(), currSelfLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.anchor(0.5f, 0.5f);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            currSelfLocationMarker = mMap.addMarker(markerOptions);
        }
        if (currDeviceLocation != null) {
            Log.i(TAG, "UpdateUI: Add marker to the map.");
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(currDeviceLocation);
            markerOptions.anchor(0.5f, 0.5f);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            currDeviceLocationMarker = mMap.addMarker(markerOptions);
        }

        if (centerMap && currSelfLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(currSelfLocation.getLatitude(), currSelfLocation.getLongitude())));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            centerMap = false;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
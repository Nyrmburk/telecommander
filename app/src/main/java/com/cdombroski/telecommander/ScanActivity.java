package com.cdombroski.telecommander;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ScanActivity extends AppCompatActivity {

    // my permissions
    // SCAN_FOR_ROBOT is used to find nearby robots with wifi scanning
    final static int MY_PERMISSION_SCAN_FOR_ROBOT = 0;
    final static String[] SCAN_FOR_ROBOT_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
    };

    ScanViewModel svm;

    WifiManager wifiManager = null;

    DeviceAdapter deviceAdapter = null;
    ProgressBar scanningProgressBar = null;
    RecyclerView nearbyDevicesList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        svm = ViewModelProviders.of(this).get(ScanViewModel.class);

        scanningProgressBar = findViewById(R.id.scanningProgressBar);
        scanningProgressBar.setIndeterminate(svm.isScanning);

        deviceAdapter = new DeviceAdapter();
        deviceAdapter.results = svm.results;

        nearbyDevicesList = findViewById(R.id.nearbyDevicesList);
        nearbyDevicesList.setHasFixedSize(false);
        nearbyDevicesList.setLayoutManager(new LinearLayoutManager(this));
        nearbyDevicesList.setAdapter(deviceAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("started");
        scanWifi();
    }

    private boolean requestPermissions(int requestCode, String... permissions) {
        boolean access = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                access = false;
                System.out.println("access denied for " + permission);
            }
        }

        if (!access) {
            ActivityCompat.requestPermissions(this, permissions, requestCode);
        }
        return access;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        boolean granted = false;
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                // If request is cancelled, the results arrays are empty.
                // only return granted true if there's at least one element
                granted = true;
            } else {
                granted = false;
                break;
            }
        }
        switch (requestCode) {
            case MY_PERMISSION_SCAN_FOR_ROBOT: {
                if (granted) {
                    scanWifi();
                } else {
                    System.out.println("sadboi. we should send a message");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void scanWifi() {
        if (wifiManager != null) {
            return;
        }
        if (!requestPermissions(MY_PERMISSION_SCAN_FOR_ROBOT, SCAN_FOR_ROBOT_PERMISSIONS)) {
            return;
        }
        System.out.println("we have all the permissions");

        Context context = getApplicationContext();
        wifiManager = (WifiManager)
                context.getSystemService(Context.WIFI_SERVICE);

        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("Got wifi scan results!");

                svm.results = wifiManager.getScanResults();
                System.out.println(svm.results.size() + " elements: " + svm.results);

                nearbyDevicesList.post(new Runnable() {
                    @Override
                    public void run() {
                        deviceAdapter.results = svm.results;
                        deviceAdapter.notifyDataSetChanged();
                    }
                });
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiScanReceiver, intentFilter);
        boolean canScan = wifiManager.startScan();
        if (canScan) {
            svm.isScanning = true;
            scanningProgressBar.setIndeterminate(true);
        }
        System.out.println("can we scan? " + canScan);
    }

    private static class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {
        private List<ScanResult> results = new ArrayList<>();
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("clicked");
            }
        };

        public static class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView name;
            public TextView bssid;

            public MyViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.deviceName);
                bssid = v.findViewById(R.id.deviceMac);
            }
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.nearby_device_layout, parent, false);
            v.setOnClickListener(mOnClickListener);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            System.out.println("binding new holder");
            ScanResult r = results.get(position);
            holder.name.setText(r.SSID);
            holder.bssid.setText(r.BSSID);
        }

        @Override
        public int getItemCount() {
            return results.size();
        }
    }
}

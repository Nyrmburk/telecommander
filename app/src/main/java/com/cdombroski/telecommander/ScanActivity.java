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

    final static String SSID_PREFIX = "kinetikos";
    final static String OUID = "a6:a0:bf";

    // my permissions
    // SCAN_FOR_ROBOT is used to find nearby robots with wifi scanning
    final static int MY_PERMISSION_SCAN_FOR_ROBOT = 0;
    final static String[] SCAN_FOR_ROBOT_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
    };

    ScanViewModel svm;

    DeviceAdapter deviceAdapter = null;
    ProgressBar scanningProgressBar = null;
    RecyclerView nearbyDevicesList = null;

    BroadcastReceiver wifiScanReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        svm = ViewModelProviders.of(this).get(ScanViewModel.class);

        scanningProgressBar = findViewById(R.id.scanningProgressBar);
        scanningProgressBar.setIndeterminate(svm.isScanning);

        deviceAdapter = new DeviceAdapter(this);
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

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("stopped");
        stopScan();
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
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
            case MY_PERMISSION_SCAN_FOR_ROBOT:
                if (granted) {
                    scanWifi();
                } else {
                    System.out.println("sadboi. we should send a message");
                }
                break;

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void scanWifi() {
        if (!requestPermissions(MY_PERMISSION_SCAN_FOR_ROBOT, SCAN_FOR_ROBOT_PERMISSIONS)) {
            return;
        }
        System.out.println("we have all the permissions");

        Context context = getApplicationContext();
        final WifiManager wifiManager = (WifiManager)
                context.getSystemService(Context.WIFI_SERVICE);

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("Got wifi scan results!");

                svm.results = findRobots(wifiManager.getScanResults());
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
        svm.isScanning = true;
        scanningProgressBar.setIndeterminate(true);
        System.out.println("can we scan? " + canScan);
    }

    private void stopScan() {
        if (wifiScanReceiver == null) {
            return;
        }
        getApplicationContext().unregisterReceiver(wifiScanReceiver);
        wifiScanReceiver = null;
        scanningProgressBar.setIndeterminate(false);
    }

    private List<ScanResult> findRobots(List<ScanResult> scanResults) {
        List<ScanResult> robots = new ArrayList<>(scanResults.size());
        for (ScanResult result : scanResults) {
            if (result.BSSID.toLowerCase().startsWith(OUID)) {
                robots.add(result);
            } else if (result.SSID.toLowerCase().startsWith(SSID_PREFIX)) {
                robots.add(result);
            }
        }
        return robots;
    }

    public void openWebInterface(ScanResult scanResult) {
        Intent intent = new Intent(this, WebInterfaceActivity.class);
        intent.putExtra("SCAN_RESULT", scanResult);
        startActivity(intent);
    }

    private static class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {
        ScanActivity scanActivity;
        private List<ScanResult> results = new ArrayList<>();

        DeviceAdapter(ScanActivity activity) {
            this.scanActivity = activity;
        }

        class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView name;
            TextView bssid;

            MyViewHolder(View v) {
                super(v);
                v.setOnClickListener(this);
                name = v.findViewById(R.id.deviceName);
                bssid = v.findViewById(R.id.deviceMac);
            }

            @Override
            public void onClick(View v) {
                ScanResult scanResult = results.get(getAdapterPosition());
                System.out.println("clicked on " + scanResult.BSSID);
                scanActivity.openWebInterface(scanResult);
            }
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.nearby_device_layout, parent, false);
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

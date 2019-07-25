package com.cdombroski.telecommander;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

public class WebInterfaceActivity extends AppCompatActivity {

    // CONNECT_TO_KINETIKOS is used to connect to a kinetikos wifi hotspot
    final static int MY_PERMISSION_CONNECT_TO_KINETIKOS = 1;
    final static String[] CONNECT_TO_KINETIKOS_PERMISSIONS = {
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
    };

    final static String KINETIKOS_ADDRESS = "http://192.168.4.1";

    private static long CONNECTING_ALERT_DELAY_MS = 100;

    WebView webInterface = null;
    ScanResult scanResult = null;

    boolean isWifiConnected = false;
    boolean isWebLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scanResult = getIntent().getParcelableExtra("SCAN_RESULT");
        connectToKinetikos(scanResult);

        // Inflating a webview takes over 100ms for some reason.
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.activity_web_interface);
                webInterface = findViewById(R.id.WebInterface);
                WebSettings webSettings = webInterface.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webInterface.setBackgroundColor(Color.TRANSPARENT);
                loadKinetikos();
            }
        });
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
            case MY_PERMISSION_CONNECT_TO_KINETIKOS:
                if (granted) {
                    connectToKinetikos(scanResult);
                } else {
                    System.out.println("Can't connect to kinetikos");
                }
                break;

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void loadKinetikos() {
        if (!isWifiConnected) {
            return;
        }
        if (webInterface != null && isWebLoaded) {
            return;
        }
        System.out.println("loading: " + KINETIKOS_ADDRESS);
        webInterface.loadUrl(KINETIKOS_ADDRESS);
        isWebLoaded = true;
    }

    private void connectToKinetikos(final ScanResult scanResult) {
        if (!requestPermissions(MY_PERMISSION_CONNECT_TO_KINETIKOS, CONNECT_TO_KINETIKOS_PERMISSIONS)) {
            return;
        }
        final WifiManager wifiManager = (WifiManager)
                getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final String ssid = "\"" + scanResult.SSID + "\"";

        // get the network id
        int networkId = -1;
        for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
            if (config.SSID.equals(ssid)) {
                networkId = config.networkId;
                break;
            }
        }
        if (networkId == -1) {
            final WifiConfiguration conf = new WifiConfiguration();
            //conf.BSSID = scanResult.BSSID;
            conf.SSID = ssid;
            // TODO check if psk needed
            final View view = getLayoutInflater().inflate(R.layout.new_wifi_network, null);
            final EditText pskEditText = view.findViewById(R.id.pskEditText);
            final Switch pskShowPassword = view.findViewById(R.id.pskShowPassword);
            pskShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    int selection = pskEditText.getSelectionStart();
                    if (checked) {
                        pskEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    } else {
                        pskEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    }
                    pskEditText.setSelection(selection);
                }
            });
            final AlertDialog pskDialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                    .setTitle(R.string.psk_dialog_title)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String psk = "\"" + pskEditText.getText() + "\"";
                            System.out.println(psk);
                            conf.preSharedKey = psk;
                            wifiManager.addNetwork(conf);
                            connectToKinetikos(scanResult);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            finish();
                        }
                    })
                    .create();
            pskEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        pskDialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
                        return true;
                    }
                    return false;
                }
            });
            pskDialog.show();
            pskDialog.getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            return;
        }

        // connect to the network
        boolean enabled = wifiManager.enableNetwork(networkId, true);
        boolean reconnected = wifiManager.reconnect();
        System.out.println(String.format("enabled: %b, reconnected: %b", enabled, reconnected));

        final ConnectivityManager connectivityManager = (ConnectivityManager)
                getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        final NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        // let the user know that we are connecting to the network in the background
        final AlertDialog connectingDialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(R.string.connecting_dialog_title)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        System.out.println("cancel");
                        finish();
                    }
                })
                .setMessage(ssid)
                .setView(new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal) {
                    { setIndeterminate(true); }
                })
                .create();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isWifiConnected) {
                    connectingDialog.show();
                }
            }
        }, CONNECTING_ALERT_DELAY_MS);

        // wait for the network to be connected
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo == null) {
                    return;
                }
                if (wifiInfo.getSSID().equals(ssid)) {
                    System.out.println(wifiInfo);
                    System.out.println("GOOD shit");
                    connectivityManager.unregisterNetworkCallback(this);
                    connectivityManager.bindProcessToNetwork(network);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isWifiConnected = true;
                            connectingDialog.dismiss();
                            loadKinetikos();
                        }
                    });
                } else {
                    System.out.println("I think wifi connection failed and we should retry");
                }
            }

            @Override
            public void onUnavailable() {
                System.out.println("unavailable");
            }

            @Override
            public void onLost(Network network) {
                System.out.println("lost");
            }
        };
        connectivityManager.registerNetworkCallback(request, callback);
    }
}
package com.cdombroski.telecommander;

import android.net.wifi.ScanResult;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ScanViewModel extends ViewModel {
    public boolean isScanning = false;
    public List<ScanResult> results = new ArrayList<>(0);
}

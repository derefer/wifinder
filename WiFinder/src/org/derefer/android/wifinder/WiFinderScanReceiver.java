package org.derefer.android.wifinder;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.util.Log;

public class WiFinderScanReceiver extends BroadcastReceiver {
    WiFinderService wiFinderService;
    
    public WiFinderScanReceiver(WiFinderService wiFinderService) {
        super();
        this.wiFinderService = wiFinderService;
    }
    
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        List<ScanResult> results = wiFinderService.wifi.getScanResults();
        for (ScanResult result : results) {
            wiFinderService.addWifi(result.SSID);
        }
        Log.d("Message", "Scan results arrived...");
    }

}

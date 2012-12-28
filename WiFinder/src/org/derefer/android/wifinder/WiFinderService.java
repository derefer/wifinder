package org.derefer.android.wifinder;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class WiFinderService extends Service {
    private final IBinder mBinder = new MyBinder();
    
    private List<String> wifiList = new ArrayList<String>();
    
    WifiManager wifi;
    
    BroadcastReceiver receiver;
    
    private boolean mIsScanning;
    
    @Override
    public void onCreate() {
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false) {
            Toast.makeText(getApplicationContext(), "WiFi is disabled... Making it enabled...", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }   
        if (receiver == null) {
            receiver = new WiFinderScanReceiver(this);
        }
    }
    
    public void setIsScanning(boolean isScanning) {
        mIsScanning = isScanning;
        if (mIsScanning) {
            registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            wifi.startScan();
            Log.d("Message", "WiFi receiver registered...");
        } else {
            unregisterReceiver(receiver);
            Log.d("Message", "WiFi receiver unregistered...");
        }
    }
    
    public boolean isScanning() {
        return mIsScanning;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.d("Message", "WiFi receiver unregistered, service destroyed...");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Message", "Still scanning...");
        wifiList.clear();
        //List<WifiConfiguration> configs = wifi.getConfiguredNetworks();
        //for (WifiConfiguration config : configs) {
        //    wifiList.add(config.SSID);
        //}
        return Service.START_NOT_STICKY;
    }

    public void addWifi(String wifi) {
        wifiList.add(wifi);
    }
    
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }
    
    public class MyBinder extends Binder {
        WiFinderService getService() {
            return WiFinderService.this;
        }
    }
    
    public List<String> getWifiList() {
        return wifiList;
    }
}

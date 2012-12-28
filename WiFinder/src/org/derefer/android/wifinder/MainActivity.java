package org.derefer.android.wifinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int menuGroupId = Menu.FIRST;
    private final int menuStartStopId = Menu.FIRST + 1;
    private final int menuPreferencesId = Menu.FIRST + 2;
    private final int menuInfoId = Menu.FIRST + 3;
    private final String menuStartTitle = "Start";
    private final String menuStopTitle = "Stop";
    private final String menuPreferencesTitle = "Preferences";
    private final String menuInfoTitle = "Info";
    
    public static final int REFRESH_CYCLE_SECS = 30;
    
    private static final String scheduleService = "org.derefer.android.wifinder.SCHEDULE_SERVICE";
    
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private WiFinderService wiFinderService; 
    
    private ListView listView;
    
    private List<String> wifiList;
    
    private ArrayAdapter<String> wifiArrayAdapter;
    
    private Timer listRefreshTimer = new Timer();
    
    // As a temporary? Doesn't seem to work that way.
    private final Handler handler = new Handler();
    
    private TimerTask listRefreshTask = new TimerTask() {
        public void run() {
            handler.post(new Runnable() {
                public void run() {
                    refreshWifiList(); 
                    Log.d("TIMER", "Timer set off...");
                }
            });
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            wiFinderService = ((WiFinderService.MyBinder)binder).getService();
            Log.d("Message", "Connected to WiFinderService...");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            wiFinderService = null;
            Log.d("Message", "Unexpected disconnect from WiFinderService...");
        }
    };
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
     }
    
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, WiFinderService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        listView = (ListView)findViewById(R.id.list_view);
        wifiList = new ArrayList<String>();
        // Create data model for list view.
        wifiArrayAdapter = new ArrayAdapter<String>(
            getApplicationContext(), 
            R.layout.list_item,
            wifiList);  
        listView.setAdapter(wifiArrayAdapter);
        listRefreshTimer.schedule(listRefreshTask, 1000, (1000 * REFRESH_CYCLE_SECS) / 4);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (wiFinderService != null) {
            unbindService(mConnection);
            wiFinderService = null;
        }
        stopListRefresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Called only once.
        // No XML for this simple menu.
        menu.add(menuGroupId, menuStartStopId, menuStartStopId, wiFinderService.isScanning() ? menuStopTitle : menuStartTitle).setIcon(wiFinderService.isScanning() ? R.drawable.ic_menu_stop : R.drawable.ic_menu_start);
        menu.add(menuGroupId, menuPreferencesId, menuPreferencesId, menuPreferencesTitle).setIcon(R.drawable.ic_menu_preferences);
        menu.add(menuGroupId, menuInfoId, menuInfoId, menuInfoTitle).setIcon(R.drawable.ic_menu_info);
        return super.onCreateOptionsMenu(menu);
    }

    // Another option would have been recreating the whole menu from two
    // different XMLs. If there's two completely different menus to load that
    // is the way...
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem startStopItem = menu.findItem(menuStartStopId);      
        if (!isMyServiceRunning()) {
            startStopItem.setTitle(menuStartTitle);
            startStopItem.setIcon(R.drawable.ic_menu_start);
        } else {
            startStopItem.setTitle(menuStopTitle);
            startStopItem.setIcon(R.drawable.ic_menu_stop);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case menuStartStopId:
            if (!wiFinderService.isScanning()) {
                // Schedule timer and start scanning.
                Intent intent = new Intent();
                intent.setAction(scheduleService);
                sendBroadcast(intent); 
                wiFinderService.setIsScanning(true);
                Toast.makeText(MainActivity.this, "Scanning started...", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(MainActivity.this, WiFinderReceiver.class);
                PendingIntent pendingIntent =  PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                pendingIntent.cancel();
                AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
                wiFinderService.setIsScanning(false);
                Toast.makeText(MainActivity.this, "Scanning stopped...", Toast.LENGTH_SHORT).show();
            }
            return true;
         case menuPreferencesId:
            Toast.makeText(MainActivity.this, "Preferences Selected", Toast.LENGTH_SHORT).show();
            return true;
         case menuInfoId:
            Toast.makeText(MainActivity.this, "Info Selected", Toast.LENGTH_SHORT).show();
            return true;
         default:
            return super.onOptionsItemSelected(item);
        }
    }    
    
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (WiFinderService.class.getName().equals(service.service.getClassName())) {
                return wiFinderService.isScanning();
            }
        }
        return false;
    }
     
    public void refreshWifiList() {
        if (wiFinderService != null && wiFinderService.isScanning()) {
            List<String> currentList = wiFinderService.getWifiList();
            if (currentList.size() == 0) {
                return;
            }
            wifiList.clear();
            // Duplicate removal, probably unnecessary.
            HashSet<String> currentSet = new HashSet<String>();
            currentSet.addAll(currentList);
            currentList.clear();
            currentList.addAll(currentSet);
            Collections.sort(currentList);
            String currentDateAndTime = mDateFormat.format(new Date());
            writeListToFile(currentList, currentDateAndTime);
            for (String entry : currentList) {
                wifiList.add(currentDateAndTime + " " + entry);
            }
            //Toast.makeText(this, "Number of SSIDs found: " + wifiList.size(), Toast.LENGTH_SHORT).show();
            wifiArrayAdapter.notifyDataSetChanged();
        }
    }
    
    public void writeListToFile(List<String> currentList, String currentDateAndTime) {
        String filePath = Environment.getExternalStorageDirectory() + File.separator + "wifinder.log";
        HashMap<String, String> fileContents = new HashMap<String, String>();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            if (inputStream != null) {
                InputStreamReader inputReader = new InputStreamReader(inputStream);
                BufferedReader buffReader = new BufferedReader(inputReader);
                String line = null;
                while ((line = buffReader.readLine()) != null) {
                    String parts[] = line.split(" ");
                    if (parts.length != 2) {
                        continue;
                    }
                    fileContents.put(parts[1].trim(), parts[0].trim());
                }
            }
        } catch (Exception e) {
            Log.d("Message", "File not found or something...");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();                    
                }
            } catch (Exception e) {
                
            }
        }
        for (String entry : currentList) {
            fileContents.put(entry, currentDateAndTime);
        }
        
        try {
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(new File(filePath)));
            for (Map.Entry<String, String> entry : fileContents.entrySet()) {
                out.write(entry.getValue() + " " + entry.getKey() + "\n");
            }
            out.close();
          } catch (Exception e) {
              Log.d("Message", "Writing failed...\n" + e.toString());
          }
    }
    
    public void stopListRefresh() {
        listRefreshTimer.cancel();
        Log.d("TIMER", "Timer cancelled...");
    }
}

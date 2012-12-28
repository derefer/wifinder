package org.derefer.android.wifinder;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WiFinderScheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // The already running service will not be restarted, but onStartCommand will be called.
        // Do we need a separate BroadcastReceiver for this? More elegant?
        AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent service = new Intent(context, WiFinderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, service, PendingIntent.FLAG_CANCEL_CURRENT);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 0);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 1000 * MainActivity.REFRESH_CYCLE_SECS, pending);
    }
}

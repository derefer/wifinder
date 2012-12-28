package org.derefer.android.wifinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// TODO: Why do we need this class?
public class WiFinderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, WiFinderService.class);
        context.startService(service);
    } 
}

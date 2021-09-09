package com.dev.stayawake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, MainActivity.CameraService.class);
        String actionName = intent.getAction();
        if (actionName.equals(AnalyticsApplication.dismiss)) {
            serviceIntent.putExtra("actionName", "dismiss");
            context.startService(serviceIntent);
        }
    }
}


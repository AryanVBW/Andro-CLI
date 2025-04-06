package com.example.reverseshell2;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.ListenableWorker;

public class ServiceStartWorker extends Worker {
    private static final String TAG = "ServiceStartWorker";

    public ServiceStartWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "ServiceStartWorker running");
        
        try {
            Context context = getApplicationContext();
            Intent serviceIntent = new Intent(context, mainService.class);
            
            // Check if we're on Android O or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            // Return success
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
            // If we fail, retry
            return Result.retry();
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        Log.d(TAG, "ServiceStartWorker stopped");
    }
}
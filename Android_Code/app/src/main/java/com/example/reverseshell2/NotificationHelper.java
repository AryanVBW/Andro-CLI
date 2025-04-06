package com.example.reverseshell2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "AndroCLI_Service";
    private static final String CHANNEL_NAME = "Background Service";
    private static final String CHANNEL_DESC = "Required for background operation";
    private static final int NOTIFICATION_ID = 1337;
    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(CHANNEL_DESC);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setShowBadge(false);

            NotificationManager notificationManager = 
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public NotificationCompat.Builder createServiceNotification(String title, String content) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setShowWhen(false)
                .setSilent(true)
                .setForegroundServiceBehavior(
                        NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    public void updateServiceNotification(String title, String content) {
        try {
            NotificationCompat.Builder builder = createServiceNotification(title, content);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
        }
    }

    public void cancelNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID);
        } catch (Exception e) {
            Log.e(TAG, "Error canceling notification", e);
        }
    }

    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }

    public static String getChannelId() {
        return CHANNEL_ID;
    }
}
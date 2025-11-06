package com.em.batterywidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat; // Requires Androidx dependencies

/**
 * This service is used to monitor the battery information.
 * CRITICAL FIX: The BroadcastReceiver now correctly uses UpdateService.enqueueWork()
 * instead of context.startService() to ensure updates are processed by the JobIntentService.
 */
public class MonitorService extends Service {

    private static final String TAG = MonitorService.class.getSimpleName();
    private static final String CHANNEL_ID = "battery_monitor_channel";
    private static final int NOTIFICATION_ID = 101; // Unique ID for the foreground notification

    final private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            // O Intent de atualização será enfileirado no UpdateService
            Intent updateIntent = new Intent(context, UpdateService.class);

            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {

                // 1. Deserializa os dados da bateria do Intent do sistema
                BatteryInfo batteryInfo = new BatteryInfo(intent);
                updateIntent.setAction(UpdateService.ACTION_BATTERY_CHANGED);

                // 2. Serializa todos os dados para o Intent do serviço.
                batteryInfo.saveToIntent(updateIntent);

                // 3. Utiliza enqueueWork para iniciar o JobIntentService de forma robusta.
                UpdateService.enqueueWork(context, updateIntent);
                Log.d(TAG, "ACTION_BATTERY_CHANGED recebido e enfileirado para UpdateService.");

            } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
                updateIntent.setAction(UpdateService.ACTION_BATTERY_LOW);
                // Utiliza enqueueWork
                UpdateService.enqueueWork(context, updateIntent);
                Log.d(TAG, "ACTION_BATTERY_LOW recebido e enfileirado para UpdateService.");


            } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
                updateIntent.setAction(UpdateService.ACTION_BATTERY_OKAY);
                // Utiliza enqueueWork
                UpdateService.enqueueWork(context, updateIntent);
                Log.d(TAG, "ACTION_BATTERY_OKAY recebido e enfileirado para UpdateService.");
            }
        }
    };

    /**
     * Creates the MonitorService.
     */
    public MonitorService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 1. Start as Foreground Service for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification());
            Log.d(TAG, "Starting as Foreground Service (API 26+).");
        } else {
            // Pre-Oreo devices can run service in background without notification
            Log.d(TAG, "Starting as regular Service (Pre-API 26).");
        }

        // 2. Register receiver with appropriate flags for modern Android
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);

        // For API 33+ (Tiramisu), the receiver needs to be explicitly marked as exported/unexported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Note: Context.RECEIVER_NOT_EXPORTED is a good default for internal app receivers
            registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    /**
     * Creates the persistent notification required for a Foreground Service (API 26+).
     * @return The Notification object.
     */
    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery Monitoring Channel", // Readable name for user
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows that the battery widget service is running.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Intent to launch the main activity when the notification is tapped
        Intent notificationIntent = new Intent(this, WidgetActivity.class); // Assuming WidgetActivity is the main screen
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE); // Use FLAG_IMMUTABLE for security

        // Build the notification
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoramento de Bateria Ativo")
                .setContentText("O widget de bateria está monitorando seu dispositivo.")
                .setSmallIcon(R.drawable.ic_launcher) // Usando o ícone do app em vez de um genérico
                .setContentIntent(pendingIntent)
                .setTicker("Monitoramento de Bateria")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Garantir que o receiver é desregistado
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Erro ao desregistar o Receiver: " + e.getMessage());
        }

        // Se foi um serviço em primeiro plano, pare-o graciosamente.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        Log.d(TAG, "MonitorService stopped and receiver unregistered.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Since we are running as a foreground service, ensure we call startForeground here
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        // Retornamos sticky para que o serviço continue a correr até ser explicitamente parado.
        return Service.START_STICKY;
    }

}
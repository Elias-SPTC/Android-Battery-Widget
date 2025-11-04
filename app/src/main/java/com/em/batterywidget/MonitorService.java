/*
 * Copyright 2015 Erkan Molla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * CRITICAL FIX: Updated to run as a Foreground Service for compatibility with modern Android OS (API 26+).
 */
public class MonitorService extends Service {

    private static final String TAG = MonitorService.class.getSimpleName();
    private static final String CHANNEL_ID = "battery_monitor_channel";
    private static final int NOTIFICATION_ID = 101; // Unique ID for the foreground notification

    final private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                BatteryInfo batteryInfo = new BatteryInfo(intent);
                Intent updateIntent = new Intent(context, UpdateService.class);
                updateIntent.setAction(UpdateService.ACTION_BATTERY_CHANGED);
                batteryInfo.saveToIntent(updateIntent);
                context.startService(updateIntent);
            } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
                Intent updateIntent = new Intent(context, UpdateService.class);
                updateIntent.setAction(UpdateService.ACTION_BATTERY_LOW);
                context.startService(updateIntent);
            } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
                Intent updateIntent = new Intent(context, UpdateService.class);
                updateIntent.setAction(UpdateService.ACTION_BATTERY_OKAY);
                context.startService(updateIntent);
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
     * NOTE: This assumes you have 'R.string.app_name', 'R.drawable.ic_stat_battery'
     * and a main Activity (like WidgetActivity) defined.
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
        // Note: You would replace R.string.app_name and R.drawable.ic_stat_battery with actual resource IDs
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoramento de Bateria Ativo") // Placeholder for R.string.app_name
                .setContentText("O widget de bateria está monitorando seu dispositivo.")
                .setSmallIcon(android.R.drawable.ic_lock_power_off) // Generic Android icon, replace with your icon
                .setContentIntent(pendingIntent)
                .setTicker("Monitoramento de Bateria")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        // If it was a foreground service, stop it gracefully.
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
        // for devices before API 26 (though the call in onCreate covers 26+).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        // We want this service to continue running until it is explicitly
        // stopped (by the last widget being removed), so return sticky.
        return Service.START_STICKY;
    }

}

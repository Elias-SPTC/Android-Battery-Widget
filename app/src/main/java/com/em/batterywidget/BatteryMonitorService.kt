package com.em.batterywidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BatteryMonitorService : Service() {

    private val batteryReceiver = BatteryBroadcastReceiver()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "BatteryMonitorChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        // Registra o receiver programaticamente para ouvir as ações de bateria
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Garante que o receiver seja desregistrado para evitar leaks de memória
        unregisterReceiver(batteryReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Serviço de Monitoramento",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Battery Widget Ativo")
            .setContentText("Monitorando o estado da bateria.")
            .setSmallIcon(R.drawable.ic_battery_status) // Você precisa de um ícone aqui
            .build()
    }
}

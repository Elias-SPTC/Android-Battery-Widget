package com.em.batterywidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.em.batterywidget.UpdateServiceUtils.getBatteryIntent
import kotlinx.coroutines.*
import kotlin.math.roundToInt

/**
 * Serviço de primeiro plano (Foreground Service) para monitorizar o estado da bateria
 * e agendar a gravação de dados de histórico.
 */
class MonitorService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "BatteryMonitorChannel"

    override fun onCreate() {
        super.onCreate()
        Log.d("MonitorService", "Service created.")
        // Inicia a notificação de primeiro plano imediatamente
        startForeground(NOTIFICATION_ID, createNotification())

        // Inicia a lógica principal
        startMonitoring()
    }

    private fun startMonitoring() {
        // Agenda o worker para gravar o histórico e atualizar os widgets a cada 15 minutos
        UpdateServiceUtils.scheduleOrCancelWork(this, true)

        // No entanto, também precisamos de uma gravação inicial e atualização imediata.
        scope.launch {
            // Garante que a primeira atualização ocorre rapidamente
            delay(1000)
            collectBatteryInfoAndNotify()
        }
    }

    /**
     * Função que realiza a coleta de dados de bateria e notifica os widgets.
     */
    private suspend fun collectBatteryInfoAndNotify() {
        // Corre no CoroutineScope (IO Thread)
        val batteryIntent = getBatteryIntent(this)
        val info = getBatteryInfoFromSystem(batteryIntent)

        // 1. Grava os dados (Lógica de base de dados omitida, apenas para fins de esqueleto)
        // saveBatteryInfoToHistory(info)

        // 2. Notifica os widgets para que se atualizem com os novos dados
        withContext(Dispatchers.Main) {
            UpdateServiceUtils.updateAllWidgets(applicationContext)
            UpdateServiceUtils.updateGraphWidgets(applicationContext)
        }

        Log.d("MonitorService", "Data collected and widgets updated. Level: ${info.level}%")
    }

    /**
     * Extrai informações detalhadas da bateria a partir do Intent.
     * Esta função estava em falta e foi adicionada aqui (necessária por WidgetActivity.kt).
     */
    fun getBatteryInfoFromSystem(batteryIntent: Intent?): BatteryExtraInfo {
        if (batteryIntent == null) {
            return BatteryExtraInfo(0, 0, 0f, 0f, "N/A")
        }

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        // A voltagem é dada em mV e a temperatura em décimos de grau Celsius.
        val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000f // em Volts
        val temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f // em Celsius
        val technology = batteryIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "N/A"

        return BatteryExtraInfo(level, scale, voltage, temperature, technology)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MonitorService", "Service started command.")
        // Se o serviço for morto, reinicie-o
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Log.d("MonitorService", "Service destroyed.")
        // Cancela o Worker quando o serviço for parado manualmente
        UpdateServiceUtils.scheduleOrCancelWork(this, false)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Cria e configura a notificação para o serviço de primeiro plano.
     */
    private fun createNotification(): Notification {
        // Cria o canal de notificação no Android O e superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name_battery_monitor)
            val descriptionText = getString(R.string.channel_description_battery_monitor)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Constrói a notificação
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitor_service_notification_title))
            .setContentText(getString(R.string.monitor_service_notification_text))
            .setSmallIcon(R.drawable.ic_battery_default) // Assumindo este recurso existe
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }

    /**
     * Estrutura de dados para informações extras da bateria,
     * usadas tanto pelo serviço quanto pela atividade de detalhes.
     */
    data class BatteryExtraInfo(
        val level: Int,
        val scale: Int,
        val voltage: Float,
        val temperature: Float,
        val technology: String
    )
}
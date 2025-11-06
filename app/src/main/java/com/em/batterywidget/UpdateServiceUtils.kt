package com.em.batterywidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Singleton object para utilitários de bateria e gerenciamento de serviço/workers.
 * A inicialização (init) é feita no AppWidgetProvider onUpdate e onReceive.
 */
object UpdateServiceUtils {

    private lateinit var applicationContext: Context
    private val BATTERY_INTENT_FILTER = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    // Constante para a saúde da bateria que estava faltando
    private const val BATTERY_HEALTH_OVERVOLTAGE = 4

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * Obtém o Intent que contém o estado atual da bateria.
     */
    fun getBatteryIntent(context: Context): Intent? {
        // null é o broadcast receiver; passando null, obtemos o valor atual
        return context.registerReceiver(null, BATTERY_INTENT_FILTER)
    }

    /**
     * Inicia o serviço de monitorização de primeiro plano.
     */
    fun startBatteryMonitorService(context: Context) {
        val serviceIntent = Intent(context, MonitorService::class.java)
        // O startForegroundService é necessário para serviços que criam notificações persistentes
        try {
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            // Em alguns dispositivos ou estados, esta chamada pode falhar.
            e.printStackTrace()
        }
    }

    /**
     * Inicia/Agenda o Worker que força a atualização dos widgets periodicamente.
     */
    fun scheduleOrCancelWork(context: Context, shouldSchedule: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val workName = "BatteryUpdateWork"

        if (shouldSchedule) {
            val updateRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
                15, TimeUnit.MINUTES // Atualiza a cada 15 minutos
            ).build()

            workManager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                updateRequest
            )
        } else {
            workManager.cancelUniqueWork(workName)
        }
    }

    /**
     * Retorna o nível da bateria em porcentagem (0-100).
     */
    fun getBatteryLevel(batteryIntent: Intent?): Int {
        if (batteryIntent == null) return 0
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (scale > 0) (level * 100 / scale.toFloat()).toInt() else 0
    }

    /**
     * Retorna um objeto BatteryStatus com informações de estado.
     */
    fun getBatteryStatus(batteryIntent: Intent?): BatteryStatus {
        if (batteryIntent == null) return BatteryStatus(
            statusText = applicationContext.getString(R.string.battery_status_unknown),
            isCharging = false,
            healthText = applicationContext.getString(R.string.battery_health_unknown),
            plugText = applicationContext.getString(R.string.battery_plugged_unplugged)
        )

        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> applicationContext.getString(R.string.battery_status_charging)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> applicationContext.getString(R.string.battery_status_discharging)
            BatteryManager.BATTERY_STATUS_FULL -> applicationContext.getString(R.string.battery_status_full)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> applicationContext.getString(R.string.battery_status_not_charging)
            else -> applicationContext.getString(R.string.battery_status_unknown)
        }

        val plugText = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> applicationContext.getString(R.string.battery_plugged_ac)
            BatteryManager.BATTERY_PLUGGED_USB -> applicationContext.getString(R.string.battery_plugged_usb)
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> applicationContext.getString(R.string.battery_plugged_wireless)
            else -> applicationContext.getString(R.string.battery_plugged_unplugged)
        }

        val healthText = mapHealthCodeToString(health)

        return BatteryStatus(statusText, isCharging, healthText, plugText)
    }

    /**
     * Mapeia o código de saúde da bateria para a string correspondente.
     * (Função necessária para WidgetActivity.kt e MonitorService.kt)
     */
    fun mapHealthCodeToString(healthCode: Int): String {
        return when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> applicationContext.getString(R.string.battery_health_good)
            BatteryManager.BATTERY_HEALTH_COLD -> applicationContext.getString(R.string.battery_health_cold)
            BatteryManager.BATTERY_HEALTH_DEAD -> applicationContext.getString(R.string.battery_health_dead)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> applicationContext.getString(R.string.battery_health_overheat)
            BATTERY_HEALTH_OVERVOLTAGE -> applicationContext.getString(R.string.battery_health_overvoltage)
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> applicationContext.getString(R.string.battery_health_failure)
            else -> applicationContext.getString(R.string.battery_health_unknown)
        }
    }

    /**
     * Força o AppWidgetManager a atualizar todos os widgets BatteryWidget.
     */
    fun updateAllWidgets(context: Context) {
        val intent = Intent(context, BatteryWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, BatteryWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Força o AppWidgetManager a atualizar todos os widgets BatteryGraphWidget (apenas gráfico).
     */
    fun updateGraphWidgets(context: Context) {
        val intent = Intent(context, BatteryGraphWidget::class.java).apply {
            action = BatteryGraphWidget.ACTION_WIDGET_UPDATE_GRAPH_ONLY
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, BatteryGraphWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }

    // Estrutura de dados para o estado da bateria
    data class BatteryStatus(
        val statusText: String,
        val isCharging: Boolean,
        val healthText: String,
        val plugText: String
    )
}
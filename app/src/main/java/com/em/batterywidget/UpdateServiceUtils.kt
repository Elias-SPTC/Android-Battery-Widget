package com.em.batterywidget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.em.batterywidget.R.string.* // Import de strings

/**
 * Utilitários para gerenciar os serviços de monitoramento e atualização.
 */
object UpdateServiceUtils {

    private const val WORK_TAG = "battery_update_work"
    private const val MONITOR_SERVICE_ACTION = "com.em.batterywidget.START_MONITOR"
    private const val WORK_NAME = "BatteryUpdateWorker"

    /**
     * Agenda o WorkManager para atualizar o widget periodicamente.
     */
    fun startUpdateWorker(context: Context) {
        // Corrigido: Unresolved references do WorkManager
        val updateRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
            15, TimeUnit.MINUTES // Intervalo de 15 minutos (mínimo permitido)
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Corrigido: Unresolved reference
            updateRequest
        )
    }

    // Assume-se que esta função ainda é necessária para o MonitorService
    fun startMonitorService(context: Context) {
        // ... (lógica de iniciar o MonitorService)
    }

    /**
     * Retorna a string de status com base no código (para uso em UpdateServiceUtils e WidgetActivity).
     * Corrigido: Resolve referências a strings em R.string
     */
    fun mapStatusCodeToString(context: Context, status: Int): String {
        return context.getString(when (status) {
            android.os.BatteryManager.BATTERY_STATUS_CHARGING -> battery_status_charging
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> battery_status_discharging
            android.os.BatteryManager.BATTERY_STATUS_FULL -> battery_status_full
            android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> battery_status_not_charging
            else -> battery_status_unknown
        })
    }
}
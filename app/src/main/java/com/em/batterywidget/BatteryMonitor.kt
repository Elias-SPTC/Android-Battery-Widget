package com.em.batterywidget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log

/**
 * Classe utilitária responsável por obter o estado atual da bateria do sistema Android
 * e salvar o log no banco de dados.
 */
object BatteryMonitor {

    // Constantes de status de bateria para uso interno (mais simples que as do BatteryManager)
    const val STATUS_UNKNOWN = 0
    const val STATUS_CHARGING = 1
    const val STATUS_DISCHARGING = 2
    const val STATUS_FULL = 3

    // Modelo de dados simples para retornar as informações da bateria
    data class BatteryInfo(
        val level: Int,
        val status: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Obtém o estado atual da bateria do sistema.
     *
     * @param context O contexto da aplicação.
     * @return Um objeto BatteryInfo com o nível e status atuais.
     */
    fun getBatteryInfo(context: Context): BatteryInfo {
        // O BatteryManager.ACTION_BATTERY_CHANGED é um sticky Intent, o que significa
        // que a última Intent enviada permanece, e podemos recuperá-la
        // passando null como BroadcastReceiver.
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, iFilter)

        // Se o Intent não for null, extraímos os dados
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (scale > 0) (level * 100 / scale) else 0

        val statusRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val status = when (statusRaw) {
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL -> STATUS_CHARGING // Se for FULL, ainda está tecnicamente "carregando/conectado"
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> STATUS_DISCHARGING
            else -> STATUS_UNKNOWN
        }

        // Se a bateria estiver cheia, definimos explicitamente como FULL
        if (statusRaw == BatteryManager.BATTERY_STATUS_FULL) {
            return BatteryInfo(100, STATUS_FULL)
        }

        return BatteryInfo(batteryPct, status)
    }

    /**
     * Obtém o estado atual da bateria e o armazena no banco de dados Room.
     *
     * @param context O contexto da aplicação.
     */
    suspend fun logBatteryState(context: Context) {
        val info = getBatteryInfo(context)
        Log.d("BatteryMonitor", "Registrando estado: Nível ${info.level}%, Status ${info.status}")

        val batteryLog = BatteryLog(
            timestampMillis = info.timestamp,
            level = info.level,
            status = info.status
        )

        try {
            val db = BatteryDatabase.getDatabase(context)
            db.batteryLogDao().insertLog(batteryLog)
            Log.i("BatteryMonitor", "Log de bateria salvo com sucesso: ${info.level}%")
        } catch (e: Exception) {
            Log.e("BatteryMonitor", "Erro ao salvar log de bateria no Room: ${e.message}", e)
        }
    }
}
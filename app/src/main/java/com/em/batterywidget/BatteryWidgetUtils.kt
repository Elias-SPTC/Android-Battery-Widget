package com.em.batterywidget.util

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import com.em.batterywidget.*
import com.em.batterywidget.R
import java.util.*

object BatteryWidgetUtils {

    /**
     * Retorna o Resource ID para o ícone de status de carregamento/plugged.
     */
    fun getStatusIcon(status: BatteryStatus, plugged: BatteryPluggedStatus): Int {
        return when (status) {
            // Ícone de carregamento se estiver carregando (independente da fonte)
            BatteryStatus.CHARGING -> R.drawable.ic_charge // Ícone de raio
            BatteryStatus.FULL -> R.drawable.ic_check // Ícone de bateria cheia (opcional)
            else -> {
                // Se não estiver carregando, verifica se está plugado para exibir o ícone de fonte
                when (plugged) {
                    BatteryPluggedStatus.AC -> R.drawable.ic_plug_ac
                    BatteryPluggedStatus.USB -> R.drawable.ic_plug_usb
                    BatteryPluggedStatus.WIRELESS -> R.drawable.ic_plug_wireless
                    BatteryPluggedStatus.NONE -> 0 // Sem ícone de status de carregamento/plugado
                }
            }
        }
    }

    /**
     * Retorna a string localizada para o status de saúde da bateria.
     */
    fun getHealthString(context: Context, health: BatteryHealthStatus): String {
        return when (health) {
            BatteryHealthStatus.GOOD -> context.getString(R.string.health_good)
            BatteryHealthStatus.OVERHEAT -> context.getString(R.string.health_overheat)
            BatteryHealthStatus.DEAD -> context.getString(R.string.health_dead)
            BatteryHealthStatus.OVER_VOLTAGE -> context.getString(R.string.health_over_voltage)
            BatteryHealthStatus.FAILURE -> context.getString(R.string.health_failure)
            BatteryHealthStatus.COLD -> context.getString(R.string.health_cold)
            BatteryHealthStatus.UNKNOWN -> context.getString(R.string.health_unknown)
        }
    }

    /**
     * Retorna a string localizada para o status de carregamento.
     */
    fun getStatusString(context: Context, status: BatteryStatus, plugged: BatteryPluggedStatus): String {
        return when (status) {
            BatteryStatus.FULL -> context.getString(R.string.status_full)
            BatteryStatus.DISCHARGING -> context.getString(R.string.status_discharging)
            BatteryStatus.NOT_CHARGING -> context.getString(R.string.status_not_charging)
            BatteryStatus.CHARGING -> {
                when (plugged) {
                    BatteryPluggedStatus.AC -> context.getString(R.string.status_charging_ac)
                    BatteryPluggedStatus.USB -> context.getString(R.string.status_charging_usb)
                    BatteryPluggedStatus.WIRELESS -> context.getString(R.string.status_charging_wireless)
                    else -> context.getString(R.string.status_charging)
                }
            }
            BatteryStatus.UNKNOWN -> context.getString(R.string.status_unknown)
        }
    }

    /**
     * Gera a string detalhada do widget baseada nas preferências.
     */
    fun getDetailsText(context: Context, info: BatteryInfo, prefs: WidgetPreferences): String {
        val details = mutableListOf<String>()

        // 1. Status de Carregamento/Conexão
        details.add(getStatusString(context, info.status, info.plugged))

        // 2. Voltagem
        if (prefs.showVoltage && info.voltage != -1) {
            details.add(
                context.getString(R.string.format_voltage, info.voltage.toString())
            )
        }

        // 3. Temperatura
        if (prefs.showTemperature && info.temperature != -1f) {
            val tempFormatted = String.format(Locale.getDefault(), "%.1f", info.temperature)
            // Assumindo Celsius
            details.add(
                context.getString(R.string.format_temperature_celsius, tempFormatted)
            )
        }

        // 4. Saúde
        if (prefs.showHealth) {
            details.add(getHealthString(context, info.health))
        }

        // 5. Tecnologia
        if (prefs.showTechnology && !info.technology.isNullOrEmpty() && info.technology != "unknown") {
            details.add(info.technology)
        }

        return details.joinToString(separator = " | ")
    }
}

// -------------------------------------------------------------------------------------------------
// Data Class para armazenar as preferências do widget.
// -------------------------------------------------------------------------------------------------

data class WidgetPreferences(
    val showVoltage: Boolean = false,
    val showTemperature: Boolean = false,
    val showHealth: Boolean = false,
    val showTechnology: Boolean = false,
    val isMonitoringEnabled: Boolean = false,
    val updateFrequency: String = "15_minutes", // Chave de preferência
    val colorTheme: String = "light",
    val graphLineColor: Int = 0xFF4CAF50.toInt(), // Cor padrão (verde)
    val showGraphFill: Boolean = true,
    val showGridLines: Boolean = true,
    val graphLineWidth: Int = 2 // Largura da linha
)
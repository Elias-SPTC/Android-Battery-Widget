package com.em.batterywidget

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import com.em.batterywidget.UpdateServiceUtils.getBatteryIntent
import com.em.batterywidget.UpdateServiceUtils.getBatteryStatus
import com.em.batterywidget.UpdateServiceUtils.mapHealthCodeToString
import com.em.batterywidget.MonitorService.BatteryExtraInfo

/**
 * Atividade para mostrar os detalhes da bateria.
 */
class WidgetActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = Runnable { updateDetails() }

    // Referências de TextView para os dados detalhados
    private lateinit var tvStatus: TextView
    private lateinit var tvPlugged: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvScale: TextView
    private lateinit var tvVoltage: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvTechnology: TextView
    private lateinit var tvHealth: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o UpdateServiceUtils
        UpdateServiceUtils.init(this)

        // Define o layout
        setContentView(R.layout.activity_widget)

        // Inicializa as TextViews
        tvStatus = findViewById(R.id.tv_status_value)
        tvPlugged = findViewById(R.id.tv_plugged_value)
        tvLevel = findViewById(R.id.tv_level_value)
        tvScale = findViewById(R.id.tv_scale_value)
        tvVoltage = findViewById(R.id.tv_voltage_value)
        tvTemperature = findViewById(R.id.tv_temperature_value)
        tvTechnology = findViewById(R.id.tv_technology_value)
        tvHealth = findViewById(R.id.tv_health_value)

        // Define o título da atividade.
        title = getString(R.string.activity_title_unavailable)
    }

    override fun onResume() {
        super.onResume()
        // Começa a atualização a cada 5 segundos
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        // Para a atualização quando a atividade está em pausa
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateDetails() {
        val batteryIntent = getBatteryIntent(this)

        if (batteryIntent != null) {
            // Obter informações detalhadas (agora usando a função do MonitorService simulada)
            val info = MonitorService().getBatteryInfoFromSystem(batteryIntent)

            // Obter status formatado
            val status = getBatteryStatus(batteryIntent)

            // Atualizar Título
            title = getString(R.string.activity_title)

            // Atualizar valores de TextView
            tvStatus.text = status.statusText
            tvPlugged.text = status.plugText

            tvLevel.text = getString(
                R.string.detail_level_format,
                info.level,
                info.scale
            )
            tvScale.text = info.scale.toString()
            tvVoltage.text = getString(
                R.string.detail_voltage_format,
                info.voltage
            )
            tvTemperature.text = getString(
                R.string.detail_temperature_format,
                info.temperature
            )
            tvTechnology.text = info.technology.ifEmpty {
                getString(R.string.battery_technology_unknown)
            }

            // Mapear código de saúde para string
            val healthCode = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, -1)
            tvHealth.text = mapHealthCodeToString(healthCode)

        } else {
            // Se o Intent for nulo (dados indisponíveis)
            val unavailable = getString(R.string.data_unavailable_short)
            title = getString(R.string.activity_title_unavailable)

            tvStatus.text = unavailable
            tvPlugged.text = unavailable
            tvLevel.text = unavailable
            tvScale.text = unavailable
            tvVoltage.text = unavailable
            tvTemperature.text = unavailable
            tvTechnology.text = unavailable
            tvHealth.text = unavailable
        }

        // Agenda a próxima atualização
        handler.postDelayed(updateRunnable, 5000)
    }
}
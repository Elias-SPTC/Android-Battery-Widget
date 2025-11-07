package com.em.batterywidget

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class para passar informações adicionais da bateria entre atividades/fragments.
 * Essencial para resolver a referência em WidgetActivity.kt.
 *
 * NOTA DE CORREÇÃO: Renomeado os campos para voltageMillivolts e temperatureCelsius
 * para corresponder à BatteryInfo, garantindo clareza sobre as unidades.
 */
@Parcelize
data class BatteryExtraInfo(
    val voltageMillivolts: Int,
    val temperatureCelsius: Float,
    val technology: String,
    val timeRemaining: Long
) : Parcelable
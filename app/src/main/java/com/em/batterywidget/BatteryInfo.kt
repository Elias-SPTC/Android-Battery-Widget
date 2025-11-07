package com.em.batterywidget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlin.math.roundToInt

/**
 * Data class to hold all relevant battery information obtained from
 * the ACTION_BATTERY_CHANGED intent.
 */
data class BatteryInfo(
    val level: Int,
    val status: BatteryStatus,
    val plugged: BatteryPluggedStatus,
    val health: BatteryHealthStatus,
    val voltage: Int, // mV
    val temperature: Float, // Degrees Celsius
    val technology: String?
) {
    /**
     * Companion object for creating a BatteryInfo instance by reading the
     * ACTION_BATTERY_CHANGED sticky intent.
     */
    companion object {
        fun create(context: Context): BatteryInfo {
            // Register receiver to get the sticky intent which holds the current battery status
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, filter)

            // Default values if the intent is somehow unavailable
            val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val pluggedInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            val levelInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scaleInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val voltageInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
            val tempInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            val technologyStr = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

            // Calculate percentage level
            val level = if (levelInt != -1 && scaleInt != -1 && scaleInt != 0) {
                ((levelInt / scaleInt.toFloat()) * 100).roundToInt()
            } else {
                -1
            }

            // Temperature is in tenths of a degree Celsius, convert to float degrees Celsius
            val temperature = tempInt / 10f

            return BatteryInfo(
                level = level,
                status = BatteryStatus.fromCode(statusInt),
                plugged = BatteryPluggedStatus.fromCode(pluggedInt),
                health = BatteryHealthStatus.fromCode(healthInt),
                voltage = voltageInt,
                temperature = temperature,
                technology = technologyStr
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// ENUMS TO MAP INT CODES TO DESCRIPTIVE STATUSES
// -------------------------------------------------------------------------------------------------

enum class BatteryStatus(val statusCode: Int) {
    CHARGING(BatteryManager.BATTERY_STATUS_CHARGING),
    DISCHARGING(BatteryManager.BATTERY_STATUS_DISCHARGING),
    NOT_CHARGING(BatteryManager.BATTERY_STATUS_NOT_CHARGING),
    FULL(BatteryManager.BATTERY_STATUS_FULL),
    UNKNOWN(BatteryManager.BATTERY_STATUS_UNKNOWN);

    companion object {
        fun fromCode(code: Int): BatteryStatus =
            values().find { it.statusCode == code } ?: UNKNOWN
    }
}

enum class BatteryPluggedStatus(val pluggedCode: Int) {
    AC(BatteryManager.BATTERY_PLUGGED_AC),
    USB(BatteryManager.BATTERY_PLUGGED_USB),
    WIRELESS(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            BatteryManager.BATTERY_PLUGGED_WIRELESS
        } else {
            -2 // Custom code for older versions where wireless might not be defined
        }
    ),
    NONE(0); // 0 means not plugged in

    companion object {
        fun fromCode(code: Int): BatteryPluggedStatus {
            // Handle the case where no constant matches (i.e., unplugged is usually code 0)
            return values().find { it.pluggedCode == code && it.pluggedCode != 0 } ?: NONE
        }
    }
}

/**
 * Maps the integer health code from BatteryManager to a descriptive status.
 */
enum class BatteryHealthStatus(val healthCode: Int) {
    GOOD(BatteryManager.BATTERY_HEALTH_GOOD),
    OVERHEAT(BatteryManager.BATTERY_HEALTH_OVERHEAT),
    DEAD(BatteryManager.BATTERY_HEALTH_DEAD),

    // FIX APLICADO AQUI: Corrigindo o erro de compilação na linha 43 (BATTERY_HEALTH_OVERVOLTAGE)
    // A constante correta é BATTERY_HEALTH_OVER_VOLTAGE (com um underscore).
    OVER_VOLTAGE(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE),

    FAILURE(BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE),
    COLD(BatteryManager.BATTERY_HEALTH_COLD),
    UNKNOWN(BatteryManager.BATTERY_HEALTH_UNKNOWN);

    companion object {
        fun fromCode(code: Int): BatteryHealthStatus =
            values().find { it.healthCode == code } ?: UNKNOWN
    }
}
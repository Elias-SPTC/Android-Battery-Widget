package com.em.batterywidget

/**
 * Classe de dados que armazena todas as informações relevantes sobre o status da bateria.
 * Isso ajuda a passar os dados da bateria de forma limpa entre serviços e atividades.
 *
 * @property level Nível de bateria atual (0-100).
 * @property scale Escala máxima da bateria (normalmente 100).
 * @property status Código de status de carregamento (ex: BATTERY_STATUS_CHARGING).
 * @property plugged Fonte de energia conectada (ex: BATTERY_PLUGGED_USB).
 * @property health Código de saúde da bateria (ex: BATTERY_HEALTH_GOOD).
 * @property voltage Voltagem atual da bateria.
 * @property temperature Temperatura atual da bateria.
 * @property technology Tecnologia da bateria (ex: "Li-ion").
 */
data class BatteryInfo(
    val level: Int = 0,
    val scale: Int = 100,
    val status: Int = android.os.BatteryManager.BATTERY_STATUS_UNKNOWN,
    val plugged: Int = 0,
    val health: Int = android.os.BatteryManager.BATTERY_HEALTH_UNKNOWN,
    val voltage: Int = 0,
    val temperature: Int = 0,
    val technology: String? = null
)
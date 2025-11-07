package com.em.batterywidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * BroadcastReceiver que escuta alterações no estado e nível da bateria.
 * Ele regista o estado detalhado da bateria na base de dados e notifica o widget para atualização.
 *
 * NOTA: Esta classe agora extrai e utiliza todos os novos parâmetros do BatteryLog (status, health, plugged, etc.).
 *
 * Requer Koin para injeção de dependência e BatteryAppWidgetProvider para notificação de atualização.
 */
class BatteryBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    // Injecção do Repositório de Bateria via Koin para acesso à base de dados
    private val repository: BatteryRepository by inject()

    // Escopo de Coroutine para operações assíncronas na base de dados (usamos Dispatchers.IO)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        private const val TAG = "BatteryReceiver"
        // Ação para indicar que o widget precisa ser atualizado (usada no AppWidgetProvider)
        const val ACTION_UPDATE_WIDGET = "com.em.batterywidget.UPDATE_WIDGET"
    }

    /**
     * Chamado sempre que um Intent com uma das ações registadas é recebido.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Ação Recebida: $action")

        when (action) {
            // Ações que indicam uma mudança de estado ou nível da bateria
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_LEVEL_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                // Captura o estado e nível da bateria e regista o log
                captureAndLogBatteryState(context)
            }
            else -> {
                // Ignorar outras ações
            }
        }
    }

    /**
     * Obtém o estado completo da bateria e o regista assincronamente na base de dados.
     * Também envia um comando para atualizar a UI do widget.
     */
    private fun captureAndLogBatteryState(context: Context) {
        // Obtém o Intent "pegajoso" que contém o estado atual da bateria
        val batteryStatus: Intent? = context.applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        // Se o Intent de estado da bateria for nulo, não podemos prosseguir
        if (batteryStatus == null) {
            Log.e(TAG, "Não foi possível obter o Intent de estado da bateria.")
            return
        }

        // --- Extração dos Parâmetros Detalhados da Bateria ---

        // Nível e Escala para calcular a porcentagem
        val level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct: Int = if (level != -1 && scale > 0) (100 * level / scale.toFloat()).toInt() else -1

        // Estado do Carregamento (BATTERY_STATUS_CHARGING, BATTERY_STATUS_FULL, etc.)
        val status: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

        // Estado de Saúde (BATTERY_HEALTH_GOOD, BATTERY_HEALTH_OVERHEAT, etc.)
        val health: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        // Tipo de Conexão (BATTERY_PLUGGED_USB, BATTERY_PLUGGED_AC, etc.)
        val plugged: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        // Tecnologia da bateria (String)
        val technology: String = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "N/A"

        // Temperatura em décimos de grau Celsius (converte para Celsius)
        val temperature: Float = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f

        // Se o nível da bateria for inválido, não logamos.
        if (batteryPct == -1) {
            Log.e(TAG, "Nível de bateria inválido, ignorando o log.")
            return
        }

        // Criação do novo objeto BatteryLog com todos os campos exigidos
        val currentLog = BatteryLog(
            level = batteryPct,
            status = status,
            health = health,
            plugged = plugged,
            technology = technology,
            temperature = temperature
            // timestamp e id são gerados automaticamente
        )

        // 1. Registar o log na base de dados de forma assíncrona
        coroutineScope.launch {
            try {
                // CHAMA O MÉTODO RENOMEADO E CORRIGIDO
                repository.addBatteryLog(currentLog)
                Log.d(TAG, "Log da bateria guardado: Nível $batteryPct%, Status: $status, Plugged: $plugged, Temperatura: $temperature°C")

                // 2. Notificar o Widget APÓS o log ser guardado com sucesso
                updateWidget(context)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao guardar o log da bateria: ${e.message}", e)
            }
        }
    }

    /**
     * Envia um broadcast para notificar o AppWidgetProvider para atualizar todos os widgets.
     */
    private fun updateWidget(context: Context) {
        // NOTA: 'BatteryAppWidgetProvider' deve ser definido separadamente para que isto funcione.
        val updateIntent = Intent(context, BatteryAppWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }
        context.sendBroadcast(updateIntent)
    }
}
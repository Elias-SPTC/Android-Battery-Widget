package com.example.batterywidget

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

/**
 * [MonitorService] é um serviço de fundo responsável por monitorizar
 * alterações no estado da bateria do sistema e sinalizar o widget para atualizar.
 */
class MonitorService : Service() {

    // Receiver interno para capturar as alterações da bateria
    private val batteryReceiver = BatteryReceiver()

    // O serviço não será ligado por outros componentes (usa Broadcasts)
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Chamado quando o serviço é iniciado (p. ex., pelo Provider ou pelo sistema).
     * Aqui, registamos o nosso BroadcastReceiver.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MonitorService", "Serviço Iniciado. A registar o Receiver de Bateria.")

        // Intent Filter para escutar a ação do sistema que notifica sobre alterações na bateria
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        // Registar o BroadcastReceiver. O ACTION_BATTERY_CHANGED é um broadcast "sticky",
        // o que significa que podemos ler os dados atuais imediatamente.
        registerReceiver(batteryReceiver, filter)

        // Usamos START_STICKY para que o Android tente reiniciar o serviço
        // se este for eliminado devido a baixa memória.
        return START_STICKY
    }

    /**
     * Chamado quando o serviço é destruído (p. ex., quando o último widget é removido).
     * É crucial desregistar o Receiver aqui.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d("MonitorService", "Serviço Destruído. A desregistar o Receiver de Bateria.")
        // Desregistar o receiver para evitar fugas de memória
        unregisterReceiver(batteryReceiver)
    }

    /**
     * BroadcastReceiver interno para receber o Intent ACTION_BATTERY_CHANGED.
     */
    private inner class BatteryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                // O estado da bateria mudou (nível, status, temperatura, etc.)
                Log.d("BatteryReceiver", "Estado da bateria alterado. A sinalizar atualização do widget.")

                // A informação real da bateria está neste Intent "sticky".
                // Para manter o código limpo, vamos apenas sinalizar o Provider
                // para que este leia o Intent 'sticky' mais recente e atualize a UI.

                // 1. Criar a Intent de Atualização Personalizada
                val updateIntent = Intent(context, BatteryWidgetProvider::class.java).apply {
                    action = BatteryWidgetProvider.ACTION_BATTERY_UPDATE
                }

                // 2. Enviar a Intent para o Provider (que chama o onReceive/onUpdate)
                context.sendBroadcast(updateIntent)
            }
        }
    }
}
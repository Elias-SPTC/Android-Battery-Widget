package com.em.batterywidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*

/**
 * Serviço de Foreground responsável por monitorar o estado da bateria em tempo real
 * através de um BroadcastReceiver dinâmico e acionar atualizações nos widgets.
 */
class MonitorService : Service() {

    private val TAG = "MonitorService"
    private val CHANNEL_ID = "BatteryMonitorChannel"
    private lateinit var batteryReceiver: BroadcastReceiver
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Variável para armazenar o último nível conhecido da bateria para evitar atualizações excessivas
    private var lastKnownLevel: Int = -1

    // --- Ciclo de Vida do Serviço ---

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MonitorService criado.")
        // Inicializa o gerenciador de notificação para criar o canal
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        setupNotificationChannel()

        // Configura e registra o BroadcastReceiver
        batteryReceiver = createBatteryBroadcastReceiver()
        registerBatteryReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "MonitorService recebido onStartCommand.")

        // Inicia como serviço de foreground
        startForeground(1, createNotification())

        // Retorna START_STICKY para garantir que o serviço seja reiniciado pelo sistema se for morto.
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "MonitorService destruído.")
        // Cancela todas as corotinas
        serviceScope.cancel()
        // Desregistra o receiver
        unregisterReceiver(batteryReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Este serviço não permite ligação
    }

    // --- Lógica do BroadcastReceiver ---

    /**
     * Cria o BroadcastReceiver que reage às mudanças de estado da bateria.
     */
    private fun createBatteryBroadcastReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    val batteryInfo = BatteryManager.getBatteryInfo(context)

                    // Verifica se o nível mudou ou se é a primeira vez
                    if (batteryInfo.level != lastKnownLevel || lastKnownLevel == -1) {
                        lastKnownLevel = batteryInfo.level
                        processBatteryUpdate(batteryInfo)
                    }
                }
            }
        }
    }

    /**
     * Registra o BroadcastReceiver para receber o estado de mudança da bateria.
     */
    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        // Suporte para Android O+ e Nível de API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Necessário para API 33+
                registerReceiver(batteryReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(batteryReceiver, filter)
            }
        } else {
            registerReceiver(batteryReceiver, filter)
        }
        Log.d(TAG, "Battery BroadcastReceiver registrado.")
    }

    /**
     * Processa as informações de bateria recém-obtidas.
     * Deve ser executado em um escopo que suporte operações de I/O (Database).
     */
    private fun processBatteryUpdate(info: BatteryInfo) {
        serviceScope.launch {
            Log.d(TAG, "Processando atualização: Nível ${info.level}%, Status: ${info.status}")

            // 1. Salvar os dados no banco de dados (para o widget gráfico)
            // Assumimos que BatteryDatabase é um Singleton ou tem um método getInstance
            BatteryDatabase.getInstance(applicationContext).saveBatteryInfo(info)

            // 2. Acionar a atualização de todos os widgets
            AppWidgetUtils.updateAllWidgets(applicationContext)
        }
    }


    // --- Configuração da Notificação de Foreground ---

    /**
     * Cria o canal de notificação para o serviço de foreground.
     */
    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name_battery_monitor),
                NotificationManager.IMPORTANCE_LOW // Importância baixa para não incomodar
            ).apply {
                description = getString(R.string.channel_description_battery_monitor)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Cria a notificação obrigatória para o serviço de foreground.
     */
    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitor_service_notification_title))
            .setContentText(getString(R.string.monitor_service_notification_text))
            .setSmallIcon(R.drawable.ic_battery_monitor) // Ícone de notificação, precisa ser criado
            .setTicker(getString(R.string.monitor_service_notification_text))
            .setOngoing(true) // Torna a notificação permanente
            .build()
    }
}
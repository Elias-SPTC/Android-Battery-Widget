package com.em.batterywidget

import android.app.Application
import android.util.Log

/**
 * Classe Application customizada para configurações de inicialização global.
 * Centraliza a inicialização do Room Database e outras configurações de serviços.
 */
class BatteryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("BatteryApplication", "Aplicação inicializada.")
        // Inicialização global de serviços (como o WorkManager) que podem ser
        // acessados via WorkManagerInitializer.

        // O Room Database será inicializado sob demanda, mas aqui é o ponto
        // ideal para qualquer configuração de bibliotecas de terceiros.

        // Garante que o WorkManager é configurado corretamente via nosso Initializer
        // que é registrado no Manifest.
    }
}
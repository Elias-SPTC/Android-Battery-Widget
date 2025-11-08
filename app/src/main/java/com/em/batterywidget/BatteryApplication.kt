package com.em.batterywidget

import android.app.Application
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Classe Application customizada para configurações de inicialização global.
 * Agora é responsável por iniciar o Koin para Injeção de Dependência.
 */
class BatteryApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializa o Koin (Kotlin dependency injection framework)
        startKoin {
            // Passa o contexto da aplicação para que as dependências
            // que precisam do Context (como Room e DataStore) possam obtê-lo.
            androidContext(this@BatteryApplication)

            // Carrega o módulo que definimos (appModule).
            // Como está no mesmo pacote, não precisa de import.
            modules(appModule)
        }

        Log.d("BatteryApplication", "Aplicação e Koin inicializados.")
    }
}

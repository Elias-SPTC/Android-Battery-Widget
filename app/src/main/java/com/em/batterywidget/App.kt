package com.em.batterywidget

import android.app.Application
import com.em.batterywidget.di.allModules // Importa a lista de todos os módulos Koin unificados
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.android.ext.koin.androidLogger

/**
 * Classe de Aplicação principal para inicializar o Koin e outros componentes globais.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicia o Koin
        startKoin {
            // Define o nível de log do Koin. Use Level.ERROR para produção ou Level.DEBUG para depuração.
            androidLogger(Level.ERROR)
            // Usa o contexto da aplicação
            androidContext(this@App)
            // Carrega a lista de todos os módulos de injeção unificados
            modules(allModules)
        }
    }
}
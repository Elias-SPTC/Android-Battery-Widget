package com.em.batterywidget

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(di)
        }

        // Agenda o trabalho periódico para atualizar a bateria
        scheduleBatteryWorker()
    }

    private fun scheduleBatteryWorker() {
        // Define as restrições (opcional, mas bom para economizar bateria)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Cria um pedido de trabalho periódico para ser executado a cada 15 minutos
        val workRequest = PeriodicWorkRequestBuilder<BatteryWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // Enfileira o trabalho, garantindo que apenas uma instância com este nome exista
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BatteryUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP, // Mantém o trabalho existente se já estiver agendado
            workRequest
        )
    }
}

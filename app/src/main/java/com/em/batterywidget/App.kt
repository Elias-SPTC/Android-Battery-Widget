package com.em.batterywidget

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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

        scheduleBatteryWorker()
        
        // A chamada para startMonitorService() foi REMOVIDA para consertar o crash na inicialização.
    }

    private fun scheduleBatteryWorker() {
        val workManager = WorkManager.getInstance(this)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val initialWorkRequest = OneTimeWorkRequestBuilder<BatteryWorker>().build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<BatteryWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        workManager.enqueue(initialWorkRequest)
        workManager.enqueueUniquePeriodicWork(
            "BatteryUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}

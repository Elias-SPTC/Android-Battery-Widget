// app/src/main/java/com/em/batterywidget/KoinModules.kt
package com.em.batterywidget

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Módulo Koin que define como criar todas as dependências da nossa aplicação.
 */
val appModule = module {

    // Definição de como criar o BatteryDatabase (Singleton)
    single {
        Room.databaseBuilder(
            androidContext(),
            BatteryDatabase::class.java,
            "battery_history_db"
        ).build()
    }

    // Definição de como criar o BatteryLogDao (obtido a partir do Database)
    factory { get<BatteryDatabase>().batteryLogDao() }

    // Definição de como criar o BatteryDataStoreManager (Singleton)
    single { BatteryDataStoreManager(androidContext()) }

    // Definição de como criar o BatteryRepository (Singleton)
    single { BatteryRepository(get(), get()) }

    // Definição de como criar o BatteryViewModel
    viewModel { BatteryViewModel(get(), get()) }

    // Definição de como criar o WidgetUpdater (Singleton)
    single { WidgetUpdater }
}

// Variável esperada por App.kt (para compatibilidade com a versão antiga)
val di = listOf(appModule)
package com.em.batterywidget.di

import androidx.room.Room
import com.em.batterywidget.BatteryDataStoreManager
import com.em.batterywidget.BatteryDatabase
import com.em.batterywidget.BatteryRepository
import com.em.batterywidget.BatteryViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Módulos Koin para injeção de dependência da aplicação, unificados e estruturados.
 * Inclui o Database, Repositório, ViewModel e DataStore.
 */

val databaseModule = module {
    // Single instance do Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            BatteryDatabase::class.java,
            "battery_log_db" // Nome do DB mantido do seu sistema
        ).build()
    }

    // Provedor para o DAO (BatteryLogDao), a partir da instância do banco de dados
    single { get<BatteryDatabase>().batteryLogDao() }
}

val repositoryModule = module {
    // Single instance do Repositório, injetando o DAO necessário
    single { BatteryRepository(get()) }
}

val viewModelModule = module {
    // ViewModel: Injeta o BatteryRepository no BatteryViewModel
    viewModel { BatteryViewModel(get()) }
}

val dataStoreModule = module {
    // Provedor para o DataStoreManager, essencial para salvar configurações do widget
    single { BatteryDataStoreManager(androidContext()) }
}

// Lista que contém todos os módulos para inicialização do Koin
val allModules = listOf(
    databaseModule,
    repositoryModule,
    viewModelModule,
    dataStoreModule // Adicionado o módulo do DataStore para injeção completa de dependências
)
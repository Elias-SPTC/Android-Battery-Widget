package com.em.batterywidget

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Classe principal do banco de dados Room para a aplicação.
 * Define as entidades (tabelas) e a versão do banco de dados.
 */
@Database(entities = [BatteryLog::class], version = 1)
abstract class BatteryDatabase : RoomDatabase() {

    /**
     * Fornece acesso ao nosso DAO (Data Access Object).
     * O Room irá implementar esta função para nós.
     */
    abstract fun batteryLogDao(): BatteryLogDao
}

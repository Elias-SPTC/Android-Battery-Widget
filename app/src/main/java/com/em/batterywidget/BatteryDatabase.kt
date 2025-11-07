package com.em.batterywidget

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database class for storing BatteryLog entries.
 * Corrige redeclaração de BatteryDatabase e fornece o método getInstance.
 */
@Database(entities = [BatteryLog::class], version = 1, exportSchema = false)
abstract class BatteryDatabase : RoomDatabase() {

    abstract fun batteryDao(): BatteryDao

    companion object {
        // Nome usado por BatteryGraphWidgetProvider.kt
        const val DATABASE_NAME = "battery_logs_db"

        @Volatile
        private var INSTANCE: BatteryDatabase? = null

        fun getDatabase(context: Context): BatteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
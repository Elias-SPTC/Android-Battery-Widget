package com.em.batterywidget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

// Extensão para aceder ao DataStore de Preferências
// Define o DataStore a nível de aplicação com o nome "battery_history_prefs"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "battery_history_prefs")

/**
 * Gerencia a leitura e escrita do histórico de bateria usando DataStore.
 * Armazena um registro simples do estado atual da bateria (nível e status de carregamento)
 * na hora mais recente.
 */
class BatteryDataStoreManager(private val context: Context) {

    // --- Chaves do DataStore ---
    private companion object {
        // Armazena uma lista JSON ou string delimitada de estados de bateria
        private val BATTERY_HISTORY_KEY = stringSetPreferencesKey("battery_history")
        // Limite máximo de registros a armazenar (ex: 7 dias * 4 registros/dia)
        private const val MAX_HISTORY_RECORDS = 50
        // Status Padrão para quando a informação não estiver disponível
        private const val DEFAULT_STATUS = "Unknown"
    }

    /**
     * Registra o estado atual da bateria no DataStore.
     * @param info Os dados atuais da bateria.
     */
    suspend fun recordBatteryState(info: BatteryInfo) {
        val timestamp = Date().time
        // Simplificar o status para um booleano de carregamento para histórico
        val isCharging = info.status == android.os.BatteryManager.BATTERY_STATUS_CHARGING

        // Formato: "timestamp:level:isCharging"
        val newRecord = "$timestamp:${info.level}:$isCharging"

        context.dataStore.edit { preferences ->
            val currentHistory = preferences[BATTERY_HISTORY_KEY] ?: emptySet()
            val newHistory = (currentHistory + newRecord)
                .sortedBy { it.substringBefore(':').toLong() } // Ordenar por tempo
                .takeLast(MAX_HISTORY_RECORDS) // Manter apenas os mais recentes

            preferences[BATTERY_HISTORY_KEY] = newHistory.toSet()
        }
    }

    /**
     * Obtém o histórico de bateria como uma lista de objetos simplificados.
     * @return Flow de lista de pares (Timestamp, Nível)
     */
    val batteryHistoryFlow: Flow<List<Pair<Long, Int>>> = context.dataStore.data
        .map { preferences ->
            val historySet = preferences[BATTERY_HISTORY_KEY] ?: emptySet()
            historySet
                .mapNotNull { record ->
                    try {
                        val parts = record.split(":")
                        if (parts.size == 3) {
                            Pair(parts[0].toLong(), parts[1].toInt())
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedBy { it.first } // Ordenar por timestamp
        }

    /**
     * Limpa todo o histórico de bateria.
     */
    suspend fun clearHistory() {
        context.dataStore.edit {
            it.remove(BATTERY_HISTORY_KEY)
        }
    }
}
package com.em.batterywidget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Define o DataStore singleton para preferências, nomeado "battery_settings"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "battery_settings")

/**
 * Gerencia o acesso e a persistência de dados do usuário (preferências) usando DataStore.
 */
object BatteryDataStoreManager {

    // --- Definição das Chaves de Preferência ---
    private val MONITOR_ENABLED_KEY = booleanPreferencesKey("monitor_enabled")
    private val UPDATE_FREQUENCY_KEY = stringPreferencesKey("update_frequency")

    // Chaves de Preferência para Gráfico (para resolver referências em SettingsFragment, etc.)
    private val GRAPH_COLOR_KEY = intPreferencesKey("graph_line_color")
    private val SHOW_FILL_KEY = booleanPreferencesKey("show_fill")
    private val SHOW_GRID_KEY = booleanPreferencesKey("show_grid")
    private val LINE_WIDTH_KEY = intPreferencesKey("line_width")

    // --- Funções de Leitura/Escrita Específicas ---

    /**
     * Define o estado de ativação do monitoramento da bateria.
     */
    suspend fun setMonitoringEnabled(context: Context, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MONITOR_ENABLED_KEY] = isEnabled
        }
    }

    /**
     * Verifica se o monitoramento da bateria está ativado.
     * Retorna 'false' como padrão.
     */
    suspend fun isMonitoringEnabled(context: Context): Boolean {
        // CORREÇÃO: O bloco `map` retorna o Boolean diretamente, e `first()` retorna o valor
        return context.dataStore.data
            .map { preferences ->
                preferences[MONITOR_ENABLED_KEY] ?: false
            }
            .first()
    }

    /**
     * Define a frequência de atualização do monitoramento.
     */
    suspend fun setUpdateFrequency(context: Context, frequency: String) {
        context.dataStore.edit { preferences ->
            preferences[UPDATE_FREQUENCY_KEY] = frequency
        }
    }

    /**
     * Obtém a frequência de atualização.
     * Retorna "15_minutes" como padrão.
     */
    suspend fun getUpdateFrequency(context: Context): String {
        return context.dataStore.data
            .map { preferences ->
                preferences[UPDATE_FREQUENCY_KEY] ?: "15_minutes"
            }
            .first()
    }

    // --- Funções Utilitárias Genéricas para Resolver Erros de Compilação em Outros Arquivos ---

    /**
     * Salva uma preferência booleana usando uma chave dinâmica (para SettingsFragment).
     */
    suspend fun saveBooleanPreference(context: Context, key: String, value: Boolean) {
        context.dataStore.edit { preferences ->
            val dataStoreKey = booleanPreferencesKey(key)
            preferences[dataStoreKey] = value
        }
    }

    /**
     * Obtém uma preferência booleana usando uma chave dinâmica (para SettingsFragment).
     */
    suspend fun getBooleanPreference(context: Context, key: String): Boolean {
        return context.dataStore.data
            .map { preferences ->
                val dataStoreKey = booleanPreferencesKey(key)
                preferences[dataStoreKey] ?: false
            }
            .first()
    }

    /**
     * Salva uma preferência Int (e.g., cor) usando uma chave dinâmica.
     */
    suspend fun saveIntPreference(context: Context, key: String, value: Int) {
        context.dataStore.edit { preferences ->
            val dataStoreKey = intPreferencesKey(key)
            preferences[dataStoreKey] = value
        }
    }

    /**
     * Obtém uma preferência Int (e.g., cor) usando uma chave dinâmica.
     */
    suspend fun getIntPreference(context: Context, key: String): Int {
        return context.dataStore.data
            .map { preferences ->
                val dataStoreKey = intPreferencesKey(key)
                preferences[dataStoreKey] ?: 0 // Retorno padrão 0 se não encontrado
            }
            .first()
    }
}
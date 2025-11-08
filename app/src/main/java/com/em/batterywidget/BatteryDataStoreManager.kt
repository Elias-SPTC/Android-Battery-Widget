package com.em.batterywidget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Cria uma instância do DataStore para as configurações dos widgets.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_settings")

/**
 * Gerencia o armazenamento e a recuperação das preferências dos widgets usando Jetpack DataStore.
 */
class BatteryDataStoreManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        // Cria uma chave de preferência única para cada ID de widget.
        fun getWidgetTypeKey(appWidgetId: Int) = intPreferencesKey("widget_type_$appWidgetId")
    }

    /**
     * Um Flow que emite um mapa de [WidgetID, TipoDeWidget] sempre que as preferências mudam.
     */
    val widgetPreferencesFlow: Flow<WidgetPreferences> = dataStore.data.map {
        val widgetTypeMap = mutableMapOf<Int, Int>()
        it.asMap().forEach { (key, value) ->
            if (key.name.startsWith("widget_type_") && value is Int) {
                try {
                    val widgetId = key.name.substringAfter("widget_type_").toInt()
                    widgetTypeMap[widgetId] = value
                } catch (e: NumberFormatException) {
                    // Ignora chaves malformadas, se houver.
                }
            }
        }
        WidgetPreferences(widgetTypeMap)
    }

    /**
     * Salva o tipo de layout escolhido para um widget específico.
     */
    suspend fun saveWidgetType(appWidgetId: Int, type: Int) {
        dataStore.edit {
            it[getWidgetTypeKey(appWidgetId)] = type
        }
    }

    /**
     * Remove as preferências de um widget que foi deletado.
     */
    suspend fun deleteWidgetType(appWidgetId: Int) {
        dataStore.edit {
            it.remove(getWidgetTypeKey(appWidgetId))
        }
    }
}

/**
 * Data class que encapsula as preferências de todos os widgets.
 */
data class WidgetPreferences(val widgetType: Map<Int, Int>)

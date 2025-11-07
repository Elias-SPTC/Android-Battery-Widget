package com.em.batterywidget

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.em.batterywidget.data.BatteryLog
import kotlinx.coroutines.launch

/**
 * ViewModel responsável por expor os dados de log da bateria para a UI.
 *
 * Utiliza o Koin para injetar o BatteryRepository.
 */
class BatteryViewModel(
    private val repository: BatteryRepository
) : ViewModel() {

    // LiveData que observa todos os logs de bateria do repositório.
    // O Flow do Room é convertido para LiveData para ser consumido pela UI.
    val allLogs: LiveData<List<BatteryLog>> = repository.getAllLogs().asLiveData(viewModelScope.coroutineContext)

    /**
     * Função para forçar a exclusão de todos os logs.
     * Útil para fins de teste ou se o usuário quiser limpar o histórico.
     */
    fun clearAllLogs() = viewModelScope.launch {
        repository.clearAllLogs()
    }
}

/**
 * Extensão para injetar o BatteryViewModel usando Koin, garantindo que
 * o módulo do ViewModel seja registrado no Koin (faremos isso a seguir).
 */
// NOTA: Esta extensão será usada na MainActivity, mas o módulo Koin do ViewModel
// precisa ser adicionado ao KoinModules.kt.
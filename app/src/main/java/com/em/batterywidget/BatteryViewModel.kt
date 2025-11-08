package com.em.batterywidget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel para a MainActivity, fornecendo dados do histórico de bateria.
 */
class BatteryViewModel(
    private val batteryRepository: BatteryRepository,
    private val dataStoreManager: BatteryDataStoreManager // Mantido para futuras preferências
) : ViewModel() {

    /**
     * Expõe o Flow de todos os logs de bateria, vindo diretamente do repositório.
     * A MainActivity observará este Flow para atualizar a UI.
     */
    val allLogs: Flow<List<BatteryLog>> = batteryRepository.getAllLogs()

    /**
     * Inicia a operação para limpar todos os logs do banco de dados.
     * A operação é delegada para o repositório e executada em uma coroutine.
     */
    fun clearAllLogs() {
        viewModelScope.launch {
            batteryRepository.clearAllLogs()
        }
    }
}

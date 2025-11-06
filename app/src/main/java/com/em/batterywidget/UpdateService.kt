package com.em.batterywidget

import android.app.IntentService
import android.content.Intent
import android.util.Log

/**
 * Este serviço foi substituído pelo MonitorService (real-time) e UpdateWorker (periodic).
 * Mantido como um stub para compatibilidade/referência, mas não executa mais lógica de atualização.
 */
class UpdateService : IntentService("UpdateService") {

    private val TAG = "UpdateServiceStub"

    // O construtor é obrigatório para IntentService

    override fun onHandleIntent(intent: Intent?) {
        Log.w(TAG, "UpdateService foi chamado, mas está obsoleto. Lógica movida para MonitorService/UpdateWorker.")
        // Não fazer nada. A chamada de retrieveHistoryData foi removida.
    }
}
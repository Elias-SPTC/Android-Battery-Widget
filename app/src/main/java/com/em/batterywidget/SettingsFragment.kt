package com.em.batterywidget

import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.em.batterywidget.utils.UpdateServiceUtils

/**
 * SettingsFragment: Tela principal de configurações do aplicativo.
 *
 * Utiliza a biblioteca PreferenceFragmentCompat para criar uma UI de configurações limpa e padronizada.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private val TAG = "SettingsFragment"
    private lateinit var dataStoreManager: BatteryDataStoreManager

    // --- Ciclo de Vida do Fragmento ---

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Carrega o XML de preferências, que define a estrutura da UI de configurações.
        setPreferencesFromResource(R.xml.settings, rootKey)

        // Inicializa o DataStore Manager
        dataStoreManager = BatteryDataStoreManager(requireContext())

        // Configura a preferência de "Ativar Monitoramento em Segundo Plano"
        setupBackgroundMonitorPreference()
    }

    override fun onResume() {
        super.onResume()
        // Certifica-se de que o resumo da preferência de monitoramento está atualizado,
        // especialmente após o usuário retornar da tela de permissões.
        updateMonitorPreferenceSummary()
    }

    // --- Lógica de Preferências ---

    /**
     * Configura o comportamento da chave de preferência para o monitoramento em segundo plano.
     */
    private fun setupBackgroundMonitorPreference() {
        val monitorPref = findPreference<Preference>(getString(R.string.key_monitor_enabled))
        monitorPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            handleMonitorSwitchClick()
            true
        }
    }

    /**
     * Manipula o clique na preferência de monitoramento.
     *
     * Se o monitoramento está sendo ativado, verifica e solicita permissões necessárias
     * (como ignorar otimizações de bateria) antes de ativar o serviço.
     */
    private fun handleMonitorSwitchClick() {
        val monitorPrefKey = getString(R.string.key_monitor_enabled)
        val isChecked = dataStoreManager.getBooleanPreference(monitorPrefKey, true)

        Log.d(TAG, "Clique no Monitoramento. Estado atual: $isChecked")

        if (isChecked) {
            // Se o usuário está desativando, apenas para o serviço.
            Log.i(TAG, "Desativando o monitoramento em segundo plano.")
            UpdateServiceUtils.stopMonitoring(requireContext())
            dataStoreManager.saveBooleanPreference(monitorPrefKey, false)
        } else {
            // Se o usuário está ativando, precisamos garantir as permissões e iniciar.
            if (BatteryWidgetUtils.isIgnoringBatteryOptimizations(requireContext())) {
                // Permissão concedida, podemos ativar e iniciar o serviço.
                Log.i(TAG, "Permissão de otimização de bateria já concedida. Ativando monitoramento.")
                dataStoreManager.saveBooleanPreference(monitorPrefKey, true)
                UpdateServiceUtils.startMonitoring(requireContext())
            } else {
                // Permissão pendente, solicitamos.
                Log.w(TAG, "Permissão de otimização de bateria pendente. Solicitando...")
                BatteryWidgetUtils.requestIgnoreBatteryOptimizations(requireActivity())
                // O estado da preferência só será salvo como 'true' após a concessão da permissão
                // (O usuário deve voltar à tela após a concessão).
            }
        }

        // Força a atualização do resumo visual
        updateMonitorPreferenceSummary()
    }

    /**
     * Atualiza o resumo (subtitle) da preferência de monitoramento de acordo com o estado das permissões.
     */
    private fun updateMonitorPreferenceSummary() {
        val monitorPrefKey = getString(R.string.key_monitor_enabled)
        val monitorPref = findPreference<Preference>(monitorPrefKey)

        if (monitorPref == null) {
            Log.e(TAG, "Preferência de Monitoramento não encontrada no layout.")
            return
        }

        val isEnabled = dataStoreManager.getBooleanPreference(monitorPrefKey, true)
        val isIgnoringOptimizations = BatteryWidgetUtils.isIgnoringBatteryOptimizations(requireContext())

        if (isEnabled) {
            monitorPref.summary = getString(R.string.summary_monitor_enabled)
            if (!isIgnoringOptimizations) {
                // Estado inconsistente: ativado no app, mas sem permissão.
                // Isso pode acontecer se a permissão foi revogada manualmente.
                monitorPref.summary = getString(R.string.summary_monitor_enabled_warning)
            }
        } else {
            monitorPref.summary = getString(R.string.summary_monitor_disabled)
        }
    }
}
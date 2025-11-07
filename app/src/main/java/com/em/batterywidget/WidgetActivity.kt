package com.em.batterywidget

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.em.batterywidget.databinding.ActivityWidgetBinding

/**
 * Activity exibida ao clicar no widget principal.
 *
 * Esta tela deve mostrar:
 * 1. O nível atual e status de carregamento.
 * 2. Detalhes estendidos (saúde, temperatura, voltagem, etc.).
 * 3. O gráfico de histórico de bateria (utilizando BatteryGraphView).
 */
class WidgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetBinding
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var dataStoreManager: BatteryDataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa as dependências
        batteryMonitor = BatteryMonitor(applicationContext)
        dataStoreManager = BatteryDataStoreManager(applicationContext)

        // Define a cor da barra de status para um tom escuro/transparente
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Observa e atualiza os dados da UI
        observeBatteryData()

        // Define o clique para o botão de configurações (para referência futura,
        // a lógica de navegação será adicionada na MainActivity)
        binding.btnSettings.setOnClickListener {
            // TODO: Adicionar navegação para MainActivity/SettingsFragment aqui
            // Por enquanto, apenas loga
            println("Configurações Clicadas!")
        }
    }

    /**
     * Coleta o BatteryInfo atual e o BatteryExtraInfo do DataStore
     * e atualiza todos os elementos da interface do usuário.
     */
    private fun observeBatteryData() {
        lifecycleScope.launch {
            // 1. Obter dados primários em tempo real
            val batteryInfo = batteryMonitor.getBatteryInfo()

            // 2. Obter dados estendidos (DataStore)
            val extraInfo = dataStoreManager.batteryExtraInfoFlow.first()

            // 3. Atualizar a UI
            updateBatteryLevel(batteryInfo)
            updateExtendedDetails(batteryInfo, extraInfo)

            // O gráfico (BatteryGraphView) tem sua própria lógica de carregamento
            // de dados do banco de dados na inicialização, mas garantimos que o LayoutManager
            // seja definido aqui para exibir os detalhes estendidos corretamente.
        }
    }

    /**
     * Atualiza o círculo de nível e a porcentagem.
     */
    private fun updateBatteryLevel(info: BatteryInfo) {
        // Assume que BatteryRenderer está disponível (será implementado mais tarde)
        // Por enquanto, apenas define os textos básicos.

        // Nível de Porcentagem
        binding.tvBatteryLevel.text = getString(R.string.battery_level_format, info.level)

        // Status de Carregamento
        val statusText = when {
            info.isCharging -> getString(R.string.status_charging)
            else -> getString(R.string.status_discharging)
        }
        binding.tvStatus.text = statusText

        // Cor do nível (para simulação antes de implementar o BatteryRenderer)
        val color = if (info.isCharging) {
            getColor(R.color.charging_color) // Roxo elétrico
        } else if (info.level < 20) {
            Color.RED
        } else {
            getColor(R.color.color_accent)
        }

        // Simulação do ícone de status de carregamento
        binding.imgChargingIcon.visibility = if (info.isCharging) android.view.View.VISIBLE else android.view.View.GONE

        // TODO: A atualização do círculo de progresso (BatteryRenderer) e do ícone de bateria
        // será feita quando o BatteryRenderer for implementado. Por enquanto, a UI básica é preenchida.
    }

    /**
     * Atualiza a seção de detalhes estendidos (saúde, temperatura, etc.).
     */
    private fun updateExtendedDetails(info: BatteryInfo, extraInfo: BatteryExtraInfo) {
        // Mapa de Health Status para String
        val healthString = when (info.health) {
            BatteryInfo.Health.GOOD -> getString(R.string.health_good)
            BatteryInfo.Health.OVERHEAT -> getString(R.string.health_overheat)
            BatteryInfo.Health.DEAD -> getString(R.string.health_dead)
            BatteryInfo.Health.OVERVOLTAGE -> getString(R.string.health_overvoltage)
            BatteryInfo.Health.FAILURE -> getString(R.string.health_failure)
            BatteryInfo.Health.COLD -> getString(R.string.health_cold)
            else -> getString(R.string.health_unknown)
        }

        // 1. Linha de Status (Saúde e Tecnologia)
        binding.tvHealthStatus.text = getString(R.string.detail_health, healthString)
        binding.tvTechnology.text = getString(R.string.detail_technology, info.technology ?: getString(R.string.unknown))

        // 2. Linha de Voltagem e Temperatura
        binding.tvVoltage.text = getString(R.string.detail_voltage, info.voltage / 1000.0)
        binding.tvTemperature.text = getString(R.string.detail_temperature, info.temperature / 10.0)

        // 3. Linha de Capacidade e Ciclos (Dados do DataStore)
        // A capacidade (design/max) e os ciclos dependem do BatteryExtraInfo
        binding.tvMaxCapacity.text = getString(R.string.detail_max_capacity, extraInfo.maxCapacity)
        binding.tvCycleCount.text = getString(R.string.detail_cycle_count, extraInfo.cycleCount)

        // 4. Última atualização
        binding.tvLastUpdate.text = getString(R.string.detail_last_update,
            UpdateServiceUtils.formatTime(System.currentTimeMillis()))
    }
}
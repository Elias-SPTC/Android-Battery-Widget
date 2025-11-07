package com.em.batterywidget

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragmento responsável por exibir o histórico de nível de bateria em um gráfico de linhas.
 */
class BatteryHistoryFragment : Fragment() {

    private lateinit var lineChart: LineChart
    private lateinit var batteryDao: BatteryDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla o layout para este fragmento
        val view = inflater.inflate(R.layout.fragment_battery_history, container, false)
        lineChart = view.findViewById(R.id.battery_line_chart)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa o DAO do banco de dados
        batteryDao = BatteryDatabase.getDatabase(requireContext()).batteryDao()

        // Configura a aparência inicial do gráfico
        setupChartAppearance()

        // Carrega e exibe os dados
        loadBatteryHistory()
    }

    /**
     * Configura as propriedades visuais do gráfico (cores, eixos, legendas).
     */
    private fun setupChartAppearance() {
        lineChart.description.isEnabled = false // Desabilita a descrição no canto
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)

        // Eixo X
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = android.graphics.Color.WHITE
        xAxis.setDrawGridLines(true)
        xAxis.granularity = 1f // Intervalo mínimo de 1 (para rótulos de tempo)
        xAxis.setAvoidFirstLastClipping(true)

        // Eixo Y (Esquerdo)
        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = android.graphics.Color.WHITE
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f // Nível da bateria é de 0 a 100
        leftAxis.setDrawGridLines(true)

        // Eixo Y (Direito) - Desabilitado
        lineChart.axisRight.isEnabled = false

        // Legenda
        val legend = lineChart.legend
        legend.textColor = android.graphics.Color.WHITE
    }

    /**
     * Carrega o histórico de bateria do banco de dados e popula o gráfico.
     */
    private fun loadBatteryHistory() {
        lifecycleScope.launch {
            val history = withContext(Dispatchers.IO) {
                // Limita a 100 os pontos de dados mais recentes para melhor desempenho de visualização
                batteryDao.getRecentHistory(100)
            }
            Log.d("BatteryHistoryFragment", "Carregado ${history.size} registros do histórico.")
            updateChart(history)
        }
    }

    /**
     * Prepara os dados do histórico para serem plotados no gráfico.
     */
    private fun updateChart(history: List<BatteryRecord>) {
        if (history.isEmpty()) {
            lineChart.setNoDataText("Nenhum dado de histórico de bateria encontrado.")
            return
        }

        // Converte os registros do Room em objetos Entry do MPAndroidChart
        val entries = history.mapIndexed { index, record ->
            // Usamos o índice (index) como valor X para plotar
            Entry(index.toFloat(), record.level.toFloat(), record)
        }

        // Cria o DataSet
        val dataSet = LineDataSet(entries, "Nível da Bateria (%)")
        dataSet.color = android.graphics.Color.parseColor("#4CAF50") // Verde
        dataSet.valueTextColor = android.graphics.Color.WHITE
        dataSet.setCircleColor(android.graphics.Color.parseColor("#8BC34A"))
        dataSet.circleRadius = 3f
        dataSet.lineWidth = 2f
        dataSet.setDrawValues(false) // Não desenha o valor em cada ponto

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        // Configura o formatador do eixo X para mostrar a hora e a data.
        val timestampFormatter = SimpleDateFormat("HH:mm\ndd/MM", Locale.getDefault())
        lineChart.xAxis.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index >= 0 && index < history.size) {
                    val timestamp = history[index].timestamp
                    return timestampFormatter.format(Date(timestamp))
                }
                return ""
            }
        }

        lineChart.invalidate() // Atualiza o gráfico na tela
    }
}
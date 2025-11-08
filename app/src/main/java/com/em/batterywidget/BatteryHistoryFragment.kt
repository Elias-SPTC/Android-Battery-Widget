package com.em.batterywidget

import android.graphics.Color
import android.os.Bundle
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragmento que exibe o histórico de nível de bateria usando a arquitetura moderna com ViewModel.
 */
class BatteryHistoryFragment : Fragment() {

    private lateinit var lineChart: LineChart
    // CORRIGIDO: Injeta o ViewModel em vez de acessar o DAO diretamente
    private val viewModel: BatteryViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_battery_history, container, false)
        lineChart = view.findViewById(R.id.battery_line_chart)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChartAppearance()
        observeBatteryHistory()
    }

    private fun setupChartAppearance() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setNoDataText("Carregando histórico...")
        lineChart.setNoDataTextColor(Color.WHITE)

        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.WHITE
            setDrawGridLines(true)
            granularity = 1f
            setAvoidFirstLastClipping(true)
        }

        lineChart.axisLeft.apply {
            textColor = Color.WHITE
            axisMinimum = 0f
            axisMaximum = 100f
            setDrawGridLines(true)
        }

        lineChart.axisRight.isEnabled = false
        lineChart.legend.textColor = Color.WHITE
    }

    /**
     * CORRIGIDO: Observa o Flow de logs do ViewModel.
     */
    private fun observeBatteryHistory() {
        lifecycleScope.launch {
            viewModel.allLogs.collectLatest { history ->
                if (history.isNotEmpty()) {
                    updateChart(history)
                } else {
                    lineChart.clear()
                    lineChart.setNoDataText("Nenhum dado de histórico de bateria encontrado.")
                    lineChart.invalidate()
                }
            }
        }
    }

    /**
     * CORRIGIDO: Usa a classe de dados correta, BatteryLog.
     */
    private fun updateChart(history: List<BatteryLog>) {
        // O gráfico espera que os dados estejam em ordem crescente de X (tempo)
        val sortedHistory = history.sortedBy { it.timestampMillis }

        val entries = sortedHistory.mapIndexed { index, log ->
            Entry(index.toFloat(), log.level.toFloat())
        }

        val dataSet = LineDataSet(entries, "Nível da Bateria (%)").apply {
            color = Color.parseColor("#4CAF50")
            valueTextColor = Color.WHITE
            setCircleColor(Color.parseColor("#8BC34A"))
            circleRadius = 3f
            lineWidth = 2f
            setDrawValues(false)
        }

        lineChart.data = LineData(dataSet)

        // Formata o eixo X para mostrar data/hora
        val timestampFormatter = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        lineChart.xAxis.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < sortedHistory.size) {
                    val timestamp = sortedHistory[index].timestampMillis
                    timestampFormatter.format(Date(timestamp))
                } else {
                    ""
                }
            }
        }

        lineChart.invalidate()
    }
}

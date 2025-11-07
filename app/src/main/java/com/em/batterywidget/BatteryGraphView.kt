package com.em.batterywidget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.em.batterywidget.db.DatabaseEntry

/**
 * BatteryGraphView: View customizada para desenhar o gráfico de histórico de nível de bateria.
 *
 * O gráfico é desenhado como uma linha conectando os pontos de nível de bateria ao longo do tempo.
 */
class BatteryGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dados do gráfico
    private var historyData: List<DatabaseEntry> = emptyList()

    // Configurações e Pincéis
    private val graphPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.graph_line_default) // Cor padrão da linha
        style = Paint.Style.STROKE
        strokeWidth = 6f // Espessura da linha em pixels
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // Pincel para desenhar a área sombreada sob a linha (opcional)
    private val fillPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.graph_fill_default)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /**
     * Define os dados de histórico a serem desenhados e força o redesenho da View.
     * @param data Uma lista de DatabaseEntry contendo o nível de bateria e o timestamp.
     */
    fun setHistoryData(data: List<DatabaseEntry>) {
        this.historyData = data
        // Força a View a chamar onDraw novamente
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (historyData.isEmpty()) {
            return
        }

        // Largura e altura da área de desenho
        val w = width.toFloat()
        val h = height.toFloat()

        // Padding para garantir que a linha não toque as bordas
        val padding = 10f
        val drawingWidth = w - 2 * padding
        val drawingHeight = h - 2 * padding

        // Determina o número de pontos (entradas no histórico)
        val numPoints = historyData.size

        // Calcula a distância horizontal entre os pontos
        // Se houver apenas 1 ponto, ele será desenhado no centro (caso especial)
        val xStep = if (numPoints > 1) drawingWidth / (numPoints - 1) else 0f

        // Objeto Path para conectar os pontos da linha
        val graphPath = Path()

        // Ponto de início do desenho
        var currentX = padding

        // 1. Inicia o path no primeiro ponto
        val firstLevel = historyData.first().level.toFloat()
        // Nível 100% mapeia para Y=padding, Nível 0% mapeia para Y=h-padding
        val firstY = padding + drawingHeight * (100f - firstLevel) / 100f
        graphPath.moveTo(currentX, firstY)

        // 2. Itera sobre os pontos restantes (se houver) e adiciona ao path
        for (i in 1 until numPoints) {
            val entry = historyData[i]
            val level = entry.level.toFloat()

            // Mapeia o nível (0-100) para a coordenada Y (inverso: 100% está no topo)
            val y = padding + drawingHeight * (100f - level) / 100f

            // Incrementa X
            currentX += xStep

            // Adiciona o ponto à linha
            graphPath.lineTo(currentX, y)
        }

        // 3. Desenha a área sombreada (preenchimento)
        if (numPoints > 0) {
            val fillPath = Path(graphPath) // Copia a linha do gráfico

            // Volta para o último ponto
            fillPath.lineTo(currentX, h - padding)

            // Desce até a parte inferior esquerda
            fillPath.lineTo(padding, h - padding)

            // Fecha o Path no ponto inicial
            fillPath.close()

            canvas.drawPath(fillPath, fillPaint)
        }

        // 4. Desenha a linha do gráfico
        canvas.drawPath(graphPath, graphPaint)
    }
}
package com.em.batterywidget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

// CORREÇÃO FINAL: Removido o import quebrado para com.em.batterywidget.db.DatabaseEntry

/**
 * BatteryGraphView: View customizada para desenhar o gráfico de histórico de nível de bateria.
 */
class BatteryGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // CORREÇÃO FINAL: Usa a classe de dados correta, BatteryLog.
    private var historyData: List<BatteryLog> = emptyList()

    private val graphPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.graph_line_default)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.graph_fill_default)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /**
     * Define os dados de histórico a serem desenhados e força o redesenho da View.
     * CORREÇÃO FINAL: O parâmetro agora é uma lista de BatteryLog.
     */
    fun setHistoryData(data: List<BatteryLog>) {
        this.historyData = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (historyData.isEmpty()) {
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 10f
        val drawingWidth = w - 2 * padding
        val drawingHeight = h - 2 * padding
        val numPoints = historyData.size
        val xStep = if (numPoints > 1) drawingWidth / (numPoints - 1) else 0f

        val graphPath = Path()
        var currentX = padding

        val firstLevel = historyData.first().level.toFloat()
        val firstY = padding + drawingHeight * (100f - firstLevel) / 100f
        graphPath.moveTo(currentX, firstY)

        for (i in 1 until numPoints) {
            val entry = historyData[i]
            val level = entry.level.toFloat()
            val y = padding + drawingHeight * (100f - level) / 100f
            currentX += xStep
            graphPath.lineTo(currentX, y)
        }

        if (numPoints > 0) {
            val fillPath = Path(graphPath)
            fillPath.lineTo(currentX, h - padding)
            fillPath.lineTo(padding, h - padding)
            fillPath.close()
            canvas.drawPath(fillPath, fillPaint)
        }

        canvas.drawPath(graphPath, graphPaint)
    }
}

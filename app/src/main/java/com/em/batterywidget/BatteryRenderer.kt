package com.em.batterywidget

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.database.Cursor
import android.graphics.*
import android.util.Log
import com.em.batterywidget.R
import java.util.ArrayList

/**
 * Classe responsável por desenhar todos os Bitmaps usados pelos widgets:
 * 1. O ícone de nível de bateria (para BatteryWidget).
 * 2. O gráfico de histórico (para BatteryGraphWidget).
 *
 * NOTA: Esta classe assume que R.drawable.lic_XX, R.drawable.battery e R.drawable.charge existem.
 * Também assume que as classes Database e BatteryInfo estão definidas.
 */
class BatteryRenderer(base: Context) : ContextWrapper(base) {

    private val TAG = "BatteryRenderer"

    // --- MÉTODOS AUXILIARES DE INFORMAÇÃO ---

    private fun mapHealthCodeToString(healthCode: Int): String {
        return when (healthCode) {
            1 -> "Desconhecida"
            2 -> "Boa"
            3 -> "Superaquecida"
            4 -> "Defeito"
            5 -> "Sobrevoltada"
            6 -> "Falha"
            7 -> "Fria"
            else -> "Desconhecida"
        }
    }

    /**
     * Formata a string de informação extra a ser exibida no rodapé do widget.
     */
    fun getBatteryExtraInfo(info: BatteryInfo?): String {
        if (info == null) return "Informação Extra Indisponível"

        val health = mapHealthCodeToString(info.health)
        val tech = info.technology

        // Formatando Voltagem e Temperatura para exibição
        val voltageV = info.voltage / 1000f // mV para V
        val tempC = info.temperature / 10f  // décimos de °C para °C

        return "Saúde: $health | Temp: ${"%.1f".format(tempC)}°C | Voltagem: ${"%.2f".format(voltageV)}V"
    }

    // --- MÉTODOS DE AJUDA PARA DESENHO ---

    /**
     * Mapeia o nível da bateria para o ID do recurso de imagem de preenchimento.
     */
    private fun getBatteryFillResource(level: Int): Int {
        // Assumindo que R.drawable.lic_XX são seus assets de imagem de preenchimento
        return when {
            level >= 95 -> R.drawable.lic_100
            level >= 85 -> R.drawable.lic_90
            level >= 75 -> R.drawable.lic_80
            level >= 65 -> R.drawable.lic_70
            level >= 55 -> R.drawable.lic_60
            level >= 45 -> R.drawable.lic_50
            level >= 35 -> R.drawable.lic_40
            level >= 25 -> R.drawable.lic_30
            level >= 15 -> R.drawable.lic_20
            level >= 5 -> R.drawable.lic_10
            else -> R.drawable.lic_10 // 0-4%
        }
    }

    /**
     * Cria um Bitmap de diagnóstico em caso de erro de carregamento ou renderização.
     */
    private fun createDiagnosticBitmap(level: Int, isCharging: Boolean, error: String): Bitmap {
        val TARGET_WIDTH = 120
        val TARGET_HEIGHT = 180
        val diagnosticBitmap = Bitmap.createBitmap(TARGET_WIDTH, TARGET_HEIGHT, Bitmap.Config.ARGB_8888)
        diagnosticBitmap.eraseColor(if (isCharging) Color.GREEN else Color.RED)
        val canvas = Canvas(diagnosticBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
        }
        val errorText = "$error $level%"
        val bounds = Rect()
        paint.getTextBounds(errorText, 0, errorText.length, bounds)
        val x = (TARGET_WIDTH - bounds.width()) / 2f
        val y = (TARGET_HEIGHT + bounds.height()) / 2f
        canvas.drawText(errorText, x, y, paint)
        return diagnosticBitmap
    }

    // --- MÉTODOS DE RENDERIZAÇÃO PRINCIPAIS ---

    /**
     * Gera um Bitmap para o gráfico de histórico de bateria (Para o Widget de Gráfico).
     */
    fun createGraphBitmap(): Bitmap {
        val GRAPH_WIDTH = 300
        val GRAPH_HEIGHT = 180

        val PADDING_LEFT = 50f
        val PADDING_TOP = 25f
        val PADDING_BOTTOM = 20f

        val DRAW_WIDTH = GRAPH_WIDTH - PADDING_LEFT - 10f
        val DRAW_HEIGHT = GRAPH_HEIGHT - PADDING_TOP - PADDING_BOTTOM

        val graphBitmap = Bitmap.createBitmap(GRAPH_WIDTH, GRAPH_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(graphBitmap)
        canvas.drawColor(Color.TRANSPARENT)

        // --- 1. LEITURA DE DADOS ---
        // Acesso ao banco de dados para obter o histórico
        val db = Database(baseContext)
        var cursor: Cursor? = null
        val levels = ArrayList<Int>()
        val voltages = ArrayList<Int>()
        val MIN_VOLTAGE = 3400 // Tensão mínima esperada (em mV)
        val MAX_VOLTAGE = 4300 // Tensão máxima esperada (em mV)
        val VOLTAGE_RANGE = MAX_VOLTAGE - MIN_VOLTAGE

        try {
            // Assume que Database.getEntries() retorna os dados ordenados do mais novo para o mais antigo
            cursor = db.getEntries()

            if (cursor != null && cursor.moveToFirst()) {
                // Assume que Database.LEVEL e Database.VOLTAGE são IDs de coluna acessíveis
                val LEVEL_COLUMN_INDEX = Database.LEVEL
                val VOLTAGE_COLUMN_INDEX = Database.VOLTAGE

                do {
                    levels.add(cursor.getInt(LEVEL_COLUMN_INDEX))
                    if (cursor.columnCount > VOLTAGE_COLUMN_INDEX && VOLTAGE_COLUMN_INDEX >= 0) {
                        voltages.add(cursor.getInt(VOLTAGE_COLUMN_INDEX))
                    } else {
                        voltages.add(MIN_VOLTAGE) // Valor padrão
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler entradas do DB para o gráfico: " + e.message)
            return createDiagnosticBitmap(0, false, "DB ERROR")
        } finally {
            cursor?.close()
            db.close()
        }

        val count = levels.size
        if (count < 1) return graphBitmap

        // Inverte as listas para que o gráfico seja desenhado da esquerda (mais antigo) para a direita (mais recente)
        levels.reverse()
        voltages.reverse()

        // --- 2. CONFIGURAÇÃO DE ESTILOS e DESENHO DA GRADE/RÓTULOS ---
        val gridPaint = Paint().apply { color = Color.parseColor("#44FFFFFF"); strokeWidth = 1f }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            textAlign = Paint.Align.RIGHT
        }

        val levelLinePaint = Paint().apply {
            color = Color.parseColor("#42A5F5") // Azul Claro
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }
        val voltageLinePaint = Paint().apply {
            color = Color.parseColor("#FFA726") // Laranja Âmbar
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }
        val levelAreaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // Desenho da Grade e Rótulos do Eixo Y (0%, 50%, 100%)
        val gridLevels = floatArrayOf(1.0f, 0.50f, 0.0f)
        for (i in gridLevels.indices) {
            val y = PADDING_TOP + DRAW_HEIGHT * (1f - gridLevels[i])
            if (i < gridLevels.size - 1) {
                // Linhas horizontais (grid)
                canvas.drawLine(PADDING_LEFT, y, GRAPH_WIDTH.toFloat(), y, gridPaint)
            }
            // Rótulos
            canvas.drawText("${(gridLevels[i] * 100).toInt()}%", PADDING_LEFT - 5f, y + textPaint.textSize / 3, textPaint)
        }

        // Linha vertical do Eixo Y
        canvas.drawLine(PADDING_LEFT, PADDING_TOP, PADDING_LEFT, GRAPH_HEIGHT - PADDING_BOTTOM, gridPaint)

        // --- 3. CÁLCULO DOS PATHS e DESENHO FINAL ---
        // Calcula o passo X para distribuir os pontos uniformemente
        val xStep = if (count > 1) DRAW_WIDTH / (count - 1).toFloat() else DRAW_WIDTH

        val levelLinePath = Path()
        val levelAreaPath = Path()
        val voltageLinePath = Path()

        // Início da área de nível no canto inferior esquerdo para preenchimento
        levelAreaPath.moveTo(PADDING_LEFT, GRAPH_HEIGHT - PADDING_BOTTOM)

        for (i in 0 until count) {
            val x = PADDING_LEFT + (i * xStep)

            // Nível (%) - Y é calculado de 0 (topo) a DRAW_HEIGHT (base)
            val levelRatio = levels[i] / 100f
            val levelY = PADDING_TOP + DRAW_HEIGHT * (1f - levelRatio)

            // Constrói o Path da Linha de Nível
            if (i == 0) levelLinePath.moveTo(x, levelY) else levelLinePath.lineTo(x, levelY)
            // Constrói o Path da Área (preenchimento)
            levelAreaPath.lineTo(x, levelY)

            // Tensão (mV) - Normaliza para o range de 0 a 100% da área de desenho
            val normalizedVoltage = Math.max(0, voltages[i] - MIN_VOLTAGE)
            val voltageRatio = Math.min(1.0f, normalizedVoltage / VOLTAGE_RANGE.toFloat())
            val voltageY = PADDING_TOP + DRAW_HEIGHT * (1f - voltageRatio)

            // Constrói o Path da Linha de Tensão
            if (i == 0) voltageLinePath.moveTo(x, voltageY) else voltageLinePath.lineTo(x, voltageY)
        }

        if (count > 0) {
            // Fecha o Path da Área para preenchimento
            levelAreaPath.lineTo(PADDING_LEFT + DRAW_WIDTH, GRAPH_HEIGHT - PADDING_BOTTOM)
            levelAreaPath.lineTo(PADDING_LEFT, GRAPH_HEIGHT - PADDING_BOTTOM)
            levelAreaPath.close()
        }

        // Desenho da Área de Nível com Gradiente
        val levelGradient = LinearGradient(0f, PADDING_TOP, 0f, GRAPH_HEIGHT - PADDING_BOTTOM, Color.parseColor("#4442A5F5"), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        levelAreaPaint.shader = levelGradient
        canvas.drawPath(levelAreaPath, levelAreaPaint)

        // Desenho das Linhas
        canvas.drawPath(levelLinePath, levelLinePaint)
        canvas.drawPath(voltageLinePath, voltageLinePaint)

        // --- 4. LEGENDA ---
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 14f }

        val LEGEND_START_X = PADDING_LEFT + 5f
        val LEGEND_Y1 = PADDING_TOP / 2 - 5f

        // Legenda Nível (%)
        levelLinePaint.style = Paint.Style.FILL // Usa o mesmo paint, mas em modo FILL para o quadrado
        canvas.drawRect(LEGEND_START_X, LEGEND_Y1, LEGEND_START_X + 10f, LEGEND_Y1 + 10f, levelLinePaint)
        canvas.drawText("Nível (%)", LEGEND_START_X + 15f, LEGEND_Y1 + 9f, legendPaint)

        // Legenda Tensão
        val VOLTAGE_LABEL_X = LEGEND_START_X + legendPaint.measureText("Nível (%)") + 35f
        voltageLinePaint.style = Paint.Style.FILL // Usa o mesmo paint, mas em modo FILL para o quadrado
        canvas.drawRect(VOLTAGE_LABEL_X, LEGEND_Y1, VOLTAGE_LABEL_X + 10f, LEGEND_Y1 + 10f, voltageLinePaint)
        canvas.drawText("Tensão", VOLTAGE_LABEL_X + 15f, LEGEND_Y1 + 9f, legendPaint)

        return graphBitmap
    }


    /**
     * Gera o Bitmap principal do ícone da bateria com porcentagem e estado de carga.
     */
    fun createBatteryBitmap(level: Int, isCharging: Boolean): Bitmap {
        val TARGET_WIDTH = 200
        val TARGET_HEIGHT = 300

        val resources: Resources = baseContext.resources
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }

        val finalBitmap = Bitmap.createBitmap(TARGET_WIDTH, TARGET_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)

        var fillBitmap: Bitmap? = null
        var shellBitmap: Bitmap? = null

        try {
            // CAMADA 1: O PREENCHIMENTO DE NÍVEL (lic_XX.png)
            val fillResId = getBatteryFillResource(level)

            if (fillResId != 0) {
                fillBitmap = BitmapFactory.decodeResource(resources, fillResId, options)
                fillBitmap?.let { nonNullFillBitmap ->
                    // Escala o bitmap de preenchimento para o tamanho alvo
                    val scaledFill = Bitmap.createScaledBitmap(nonNullFillBitmap, TARGET_WIDTH, TARGET_HEIGHT, true)
                    canvas.drawBitmap(scaledFill, 0f, 0f, null)
                    scaledFill.recycle()
                }
            }

            // --- CAMADA 2: DESENHO DO ÍCONE DE CARGA (Raio) ---
            if (isCharging) {
                var chargeBitmap: Bitmap? = null
                try {
                    // Assumindo R.drawable.charge é o ícone de raio
                    chargeBitmap = BitmapFactory.decodeResource(resources, R.drawable.charge, options)
                    chargeBitmap?.let { nonNullChargeBitmap ->
                        val CHARGE_WIDTH = (TARGET_WIDTH * 0.7).toInt()
                        val CHARGE_HEIGHT = (TARGET_HEIGHT * 0.4).toInt()

                        val scaledCharge = Bitmap.createScaledBitmap(nonNullChargeBitmap, CHARGE_WIDTH, CHARGE_HEIGHT, true)

                        val chargeX = (TARGET_WIDTH - CHARGE_WIDTH) / 2
                        val chargeY = (TARGET_HEIGHT - CHARGE_HEIGHT) / 2

                        canvas.drawBitmap(scaledCharge, chargeX.toFloat(), chargeY.toFloat(), null)
                        scaledCharge.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao desenhar ícone de carga no bitmap: " + e.message)
                } finally {
                    chargeBitmap?.let { if (!it.isRecycled) it.recycle() }
                }
            }

            // CAMADA 3: O CONTORNO (battery.png)
            shellBitmap = BitmapFactory.decodeResource(resources, R.drawable.battery, options)
            if (shellBitmap == null) {
                return createDiagnosticBitmap(level, isCharging, "FAIL SHELL")
            }
            // Escala o contorno
            val scaledShell = Bitmap.createScaledBitmap(shellBitmap, TARGET_WIDTH, TARGET_HEIGHT, true)
            canvas.drawBitmap(scaledShell, 0f, 0f, null)
            scaledShell.recycle()


            // --- CAMADA 4: O TEXTO (Porcentagem) ---
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setShadowLayer(6f, 3f, 3f, Color.BLACK) // Adiciona sombra para destaque
            }

            val textSize = TARGET_HEIGHT * 0.25f // Tamanho do texto proporcional
            paint.textSize = textSize

            val text = "$level%"
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)

            // Centraliza o texto
            val x = (finalBitmap.width - bounds.width()) / 2
            val y = (finalBitmap.height + bounds.height()) / 2 + (finalBitmap.height * 0.05f).toInt()

            canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

        } catch (e: Exception) {
            Log.e(TAG, "Erro geral ao criar o Bitmap da bateria: " + e.message)
            return createDiagnosticBitmap(level, isCharging, "FAIL EXCEPTION")
        } finally {
            // Garante que os Bitmaps temporários sejam liberados
            fillBitmap?.let { if (!it.isRecycled) it.recycle() }
            shellBitmap?.let { if (!it.isRecycled) it.recycle() }
        }

        return finalBitmap
    }
}
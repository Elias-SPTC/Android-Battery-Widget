package com.em.batterywidget

import android.graphics.*
import android.os.BatteryManager

/**
 * Objeto utilitário para renderizar os componentes visuais dos widgets de bateria.
 */
object BatteryRenderer {

    /**
     * Cria um Bitmap que representa o estado atual da bateria.
     * CORRIGIDO: O parâmetro 'context' não utilizado foi removido.
     */
    fun createBatteryBitmap(info: BatteryLog): Bitmap {
        val width = 200
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = if (info.level <= 15) Color.RED else Color.DKGRAY

        val body = RectF(10f, 50f, width - 10f, height - 10f)
        canvas.drawRect(body, paint)

        val terminal = RectF(width * 0.4f, 20f, width * 0.6f, 50f)
        canvas.drawRect(terminal, paint)

        paint.color = Color.GREEN
        val levelHeight = (body.height() - 20) * (info.level / 100.0f)
        val levelRect = RectF(
            body.left + 10f,
            body.bottom - 10f - levelHeight,
            body.right - 10f,
            body.bottom - 10f
        )
        canvas.drawRect(levelRect, paint)

        if (info.status == BatteryManager.BATTERY_STATUS_CHARGING) {
            paint.color = Color.YELLOW
            val path = Path().apply {
                moveTo(width * 0.5f, height * 0.4f)
                lineTo(width * 0.4f, height * 0.6f)
                lineTo(width * 0.6f, height * 0.6f)
                lineTo(width * 0.5f, height * 0.8f)
            }
            canvas.drawPath(path, paint)
        }

        return bitmap
    }
}

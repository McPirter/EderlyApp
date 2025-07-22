package com.example.elderly

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentData: List<Float> = emptyList()
        private set

    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val pointPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val axisPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
        textSize = 24f
    }

    fun setData(data: List<Float>) {
        currentData = data
        Log.d("LineChart", "Datos recibidos: ${data.joinToString()}")
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (currentData.isEmpty()) {
            canvas.drawText("No hay datos", width/2f, height/2f, axisPaint)
            return
        }

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 50f
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        // Ejes
        canvas.drawLine(padding, height - padding, width - padding, height - padding, axisPaint)
        canvas.drawLine(padding, height - padding, padding, padding, axisPaint)

        // Escala
        val maxY = currentData.maxOrNull() ?: 100f
        val minY = currentData.minOrNull() ?: 0f
        val rangeY = maxY - minY
        val scaleY = if (rangeY > 0) chartHeight / rangeY else 1f

        // Puntos y línea
        val path = Path()
        val stepX = chartWidth / (currentData.size - 1).coerceAtLeast(1)

        currentData.forEachIndexed { index, value ->
            val x = padding + index * stepX
            val y = height - padding - (value - minY) * scaleY

            // Punto
            canvas.drawCircle(x, y, 8f, pointPaint)

            // Valor
            canvas.drawText(
                "%.1f".format(value),
                x,
                y - 15f,
                axisPaint.apply { textSize = 12f }
            )

            // Línea
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        canvas.drawPath(path, paint)
    }
}
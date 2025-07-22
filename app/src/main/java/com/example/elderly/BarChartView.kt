package com.example.elderly

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataPoints = mutableListOf<Float>() // Datos de temperatura (ejemplo: [36.2f, 36.5f...])
    private val barPaint = Paint().apply {
        color = Color.parseColor("#FFA500")
        style = Paint.Style.FILL
    }
    private val axisPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
        textSize = 24f
    }

    var currentData: List<Float> = emptyList()
        private set

    fun setData(data: List<Float>) {
        currentData = data
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

        // Escala (temperatura normalmente entre 35-40°C)
        val maxY = currentData.maxOrNull() ?: 40f
        val minY = currentData.minOrNull() ?: 35f
        val rangeY = maxY - minY
        val scaleY = if (rangeY > 0) chartHeight / rangeY else 1f

        // Barras
        val barWidth = (chartWidth / currentData.size) * 0.8f
        val gap = (chartWidth / currentData.size) * 0.2f

        currentData.forEachIndexed { index, value ->
            val left = padding + index * (barWidth + gap)
            val top = height - padding - (value - minY) * scaleY
            val right = left + barWidth
            val bottom = height - padding

            canvas.drawRect(left, top, right, bottom, barPaint)

            // Valor
            canvas.drawText(
                "%.1f°".format(value),
                left + barWidth/2,
                top - 10f,
                axisPaint.apply {
                    textSize = 12f
                    textAlign = Paint.Align.CENTER
                }
            )
        }
    } 
}
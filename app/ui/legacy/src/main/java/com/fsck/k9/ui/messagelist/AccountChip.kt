package com.fsck.k9.ui.messagelist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class AccountChip(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val textPaint = TextPaint().apply {
        color = Color.BLUE
    }

    init {
        setTextSizeSp(16)
    }

    fun setTextSizeSp(size: Int) {
        val displayMetrics = context.resources.displayMetrics
        textPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size.toFloat(), displayMetrics)
    }

    fun setColor(color: Int) {
        textPaint.color = color
    }

    override fun onDraw(canvas: Canvas) {
        val cornerRadius = width.toFloat() / 2
        val baseLine = height - textPaint.fontMetrics.bottom

        val chipTop = baseLine + textPaint.fontMetrics.ascent
        val chipBottom = baseLine + cornerRadius

        canvas.drawRoundRect(0f, chipTop, width.toFloat(), chipBottom, cornerRadius, cornerRadius, textPaint)
    }
}

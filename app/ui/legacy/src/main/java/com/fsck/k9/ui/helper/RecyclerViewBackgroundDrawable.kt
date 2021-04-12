package com.fsck.k9.ui.helper

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView

// Based on https://stackoverflow.com/a/49825927/1800174
class RecyclerViewBackgroundDrawable internal constructor(private val color: Int) : Drawable() {
    private var recyclerView: RecyclerView? = null
    private val paint: Paint = Paint().apply {
        color = this@RecyclerViewBackgroundDrawable.color
    }

    fun attachTo(recyclerView: RecyclerView?) {
        this.recyclerView = recyclerView
        recyclerView?.background = this
    }

    override fun draw(canvas: Canvas) {
        val recyclerView = recyclerView ?: return
        if (recyclerView.childCount == 0) {
            return
        }

        var bottom = recyclerView.getChildAt(recyclerView.childCount - 1).bottom
        if (bottom >= recyclerView.bottom) {
            bottom = recyclerView.bottom
        }

        canvas.drawRect(
            recyclerView.left.toFloat(),
            recyclerView.top.toFloat(),
            recyclerView.right.toFloat(),
            bottom.toFloat(),
            paint
        )
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

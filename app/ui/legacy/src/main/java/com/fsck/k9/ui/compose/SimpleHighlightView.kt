/*
 * Copyright 2020 The K-9 Dog Walkers
 *
 * Based on ShowcaseView (https://github.com/amlcurran/ShowcaseView)
 * Copyright 2014 Alex Curran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fsck.k9.ui.compose

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.fsck.k9.ui.R

/**
 * A view which allows you to highlight a view in your Activity.
 */
class SimpleHighlightView private constructor(context: Context, style: Int) : FrameLayout(context) {
    private val backgroundColor: Int

    private val fadeInMillis = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
    private val fadeOutMillis = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

    private val radius: Float = resources.getDimension(R.dimen.highlight_radius)

    private val basicPaint = Paint()
    private val eraserPaint = Paint().apply {
        color = 0xFFFFFF
        alpha = 0
        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        isAntiAlias = true
    }

    private val positionInWindow = IntArray(2)
    private var parent: ViewGroup? = null
    private var highlightX = -1
    private var highlightY = -1
    private var bitmapBuffer: Bitmap? = null

    init {
        val styled = getContext().obtainStyledAttributes(style, R.styleable.SimpleHighlightView)
        backgroundColor = styled.getColor(
            R.styleable.SimpleHighlightView_highlightBackgroundColor,
            Color.argb(128, 80, 80, 80),
        )
        styled.recycle()
    }

    fun remove() {
        fadeOutHighlightAndRemoveFromParent()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val highlightX = this.highlightX
        val highlightY = this.highlightY
        val bitmapBuffer = this.bitmapBuffer

        if (highlightX < 0 || highlightY < 0 || bitmapBuffer == null) {
            super.dispatchDraw(canvas)
            return
        }

        // Draw background color
        erase(bitmapBuffer)

        drawHighlightCircle(bitmapBuffer, highlightX.toFloat(), highlightY.toFloat())
        drawToCanvas(canvas, bitmapBuffer)

        super.dispatchDraw(canvas)
    }

    private fun erase(bitmapBuffer: Bitmap) {
        bitmapBuffer.eraseColor(backgroundColor)
    }

    private fun drawHighlightCircle(buffer: Bitmap, x: Float, y: Float) {
        Canvas(buffer).apply {
            drawCircle(x, y, radius, eraserPaint)
        }
    }

    private fun drawToCanvas(canvas: Canvas, bitmapBuffer: Bitmap) {
        canvas.drawBitmap(bitmapBuffer, 0f, 0f, basicPaint)
    }

    private fun setHighlightPosition(x: Int, y: Int) {
        getLocationInWindow(positionInWindow)
        highlightX = x - positionInWindow[0]
        highlightY = y - positionInWindow[1]
        invalidate()
    }

    private fun setParent(parent: ViewGroup) {
        this.parent = parent
    }

    private fun setTarget(targetView: View) {
        postDelayed(
            {
                if (canUpdateBitmap()) {
                    updateBitmap()
                }

                val point = targetView.getHighlightPoint()
                setHighlightPosition(point.x, point.y)
            },
            100,
        )
    }

    private fun canUpdateBitmap(): Boolean {
        return measuredHeight > 0 && measuredWidth > 0
    }

    private fun updateBitmap() {
        bitmapBuffer.let { bitmapBuffer ->
            if (bitmapBuffer == null || bitmapBuffer.haveBoundsChanged()) {
                bitmapBuffer?.recycle()
                this.bitmapBuffer = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
            }
        }
    }

    private fun Bitmap.haveBoundsChanged(): Boolean {
        return measuredWidth != width || measuredHeight != height
    }

    private fun View.getHighlightPoint(): Point {
        val location = IntArray(2)
        getLocationInWindow(location)
        val x = location[0] + width / 2
        val y = location[1] + height / 2
        return Point(x, y)
    }

    private fun clearBitmap() {
        bitmapBuffer?.let { bitmapBuffer ->
            if (!bitmapBuffer.isRecycled) {
                bitmapBuffer.recycle()
                this.bitmapBuffer = null
            }
        }
    }

    private fun show() {
        if (canUpdateBitmap()) {
            updateBitmap()
        }

        fadeInHighlight()
    }

    private fun fadeInHighlight() {
        ObjectAnimator.ofFloat(this, ALPHA, INVISIBLE, VISIBLE)
            .setDuration(fadeInMillis)
            .onAnimationStart { isVisible = true }
            .start()
    }

    private fun fadeOutHighlightAndRemoveFromParent() {
        ObjectAnimator.ofFloat(this, ALPHA, INVISIBLE)
            .setDuration(fadeOutMillis)
            .onAnimationEnd {
                isVisible = false
                clearBitmap()
                parent?.removeView(this@SimpleHighlightView)
            }
            .start()
    }

    private inline fun ObjectAnimator.onAnimationStart(crossinline block: () -> Unit): ObjectAnimator {
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    block()
                }
            },
        )
        return this
    }

    private inline fun ObjectAnimator.onAnimationEnd(crossinline block: () -> Unit): ObjectAnimator {
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    block()
                }
            },
        )
        return this
    }

    companion object {
        private const val ALPHA = "alpha"
        private const val INVISIBLE = 0f
        private const val VISIBLE = 1f

        @JvmStatic
        fun createAndInsert(activity: Activity, targetView: View, style: Int): SimpleHighlightView {
            val highlightView = SimpleHighlightView(activity, style)
            highlightView.setTarget(targetView)

            val parent = activity.findViewById<ViewGroup>(android.R.id.content)
            highlightView.setParent(parent)

            val parentIndex = parent.childCount
            parent.addView(highlightView, parentIndex)

            highlightView.show()

            return highlightView
        }
    }
}

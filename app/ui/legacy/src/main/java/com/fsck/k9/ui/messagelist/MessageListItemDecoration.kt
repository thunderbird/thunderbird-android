package com.fsck.k9.ui.messagelist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.withSave
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.fsck.k9.ui.resolveDrawableAttribute
import kotlin.math.roundToInt

/**
 * An [ItemDecoration] that uses the alpha and visibility values of a view when drawing the divider.
 *
 * Based on [androidx.recyclerview.widget.DividerItemDecoration].
 */
class MessageListItemDecoration(context: Context) : ItemDecoration() {
    private val divider: Drawable = context.theme.resolveDrawableAttribute(android.R.attr.listDivider)
    private val bounds = Rect()

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null) return

        canvas.withSave {
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                if (!child.isVisible) {
                    continue
                }

                parent.getDecoratedBoundsWithMargins(child, bounds)

                val left = 0
                val right = parent.width
                val bottom = bounds.bottom + child.translationY.roundToInt()
                val top = bottom - divider.intrinsicHeight

                divider.setBounds(left, top, right, bottom)
                divider.alpha = (child.alpha * 255).toInt()
                divider.draw(canvas)
            }
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(0, 0, 0, divider.intrinsicHeight)
    }
}

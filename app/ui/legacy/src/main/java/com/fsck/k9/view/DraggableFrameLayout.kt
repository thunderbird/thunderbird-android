package com.fsck.k9.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.android.material.R

private val DRAGGED_STATE_SET = intArrayOf(
    R.attr.state_dragged,
    // When the item is dragged we also set the 'pressed' state so the item background is changed
    android.R.attr.state_pressed,
)

class DraggableFrameLayout : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var isDragged: Boolean = false
        set(value) {
            field = value
            refreshDrawableState()
            invalidate()
        }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + DRAGGED_STATE_SET.size)
        if (isDragged) {
            mergeDrawableStates(drawableState, DRAGGED_STATE_SET)
        }
        return drawableState
    }
}

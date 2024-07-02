package com.fsck.k9.ui.compose

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.fsck.k9.view.RecipientSelectView
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Custom [CircleImageView] used in recipient chip layout to allow us to invalidate the [RecipientSelectView].
 */
class RecipientCircleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : CircleImageView(context, attrs, defStyleAttr) {
    var onSetImageDrawableListener: OnSetImageDrawableListener? = null

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        onSetImageDrawableListener?.onSetImageDrawable()
    }
}

interface OnSetImageDrawableListener {
    fun onSetImageDrawable()
}

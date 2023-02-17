package com.fsck.k9.ui.compose

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Custom [ConstraintLayout] that returns an appropriate baseline value for our recipient token layout.
 */
class RecipientTokenConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private lateinit var textView: TextView

    override fun onFinishInflate() {
        super.onFinishInflate()
        textView = findViewById(android.R.id.text1)
    }

    override fun getBaseline(): Int {
        return textView.top + textView.baseline
    }
}

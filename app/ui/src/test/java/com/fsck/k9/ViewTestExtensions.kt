package com.fsck.k9

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView


val TextView.textString: String
    get() = text.toString()

val View.backgroundColor: Int
    get() = (background as ColorDrawable).color

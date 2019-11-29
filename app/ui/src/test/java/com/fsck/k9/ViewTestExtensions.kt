package com.fsck.k9

import android.widget.TextView

val TextView.textString: String
    get() = text.toString()

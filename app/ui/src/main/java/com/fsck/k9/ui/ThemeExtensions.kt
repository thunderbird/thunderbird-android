package com.fsck.k9.ui

import android.content.res.Resources.Theme
import android.util.TypedValue

fun Theme.resolveAttribute(resId: Int): Int {
    val typedValue = TypedValue()

    val found = resolveAttribute(resId, typedValue, true)
    if (!found) {
        throw IllegalStateException("Couldn't resolve attribute ($resId)")
    }

    return typedValue.data
}

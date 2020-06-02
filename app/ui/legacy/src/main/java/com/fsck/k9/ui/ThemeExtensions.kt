package com.fsck.k9.ui

import android.content.res.Resources.Theme
import android.graphics.drawable.Drawable
import android.util.TypedValue

fun Theme.resolveColorAttribute(attrId: Int): Int {
    val typedValue = TypedValue()

    val found = resolveAttribute(attrId, typedValue, true)
    if (!found) {
        throw IllegalStateException("Couldn't resolve attribute ($attrId)")
    }

    return typedValue.data
}

fun Theme.resolveDrawableAttribute(attrId: Int): Drawable {
    val typedValue = TypedValue()

    val found = resolveAttribute(attrId, typedValue, true)
    if (!found) {
        throw IllegalStateException("Couldn't resolve attribute ($attrId)")
    }

    return getDrawable(typedValue.resourceId)
}

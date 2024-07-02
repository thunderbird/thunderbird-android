package com.fsck.k9.ui

import android.content.res.Resources.Theme
import android.graphics.Color
import android.util.TypedValue

fun Theme.resolveColorAttribute(attrId: Int): Int {
    val typedValue = TypedValue()

    val found = resolveAttribute(attrId, typedValue, true)
    if (!found) {
        throw IllegalStateException("Couldn't resolve attribute ($attrId)")
    }

    return typedValue.data
}

fun Theme.resolveColorAttribute(colorAttrId: Int, alphaFractionAttrId: Int, backgroundColorAttrId: Int): Int {
    val typedValue = TypedValue()

    if (!resolveAttribute(colorAttrId, typedValue, true)) {
        error("Couldn't resolve attribute ($colorAttrId)")
    }
    val color = typedValue.data

    if (!resolveAttribute(alphaFractionAttrId, typedValue, true)) {
        error("Couldn't resolve attribute ($alphaFractionAttrId)")
    }
    val colorPercentage = TypedValue.complexToFloat(typedValue.data)
    val backgroundPercentage = 1 - colorPercentage

    if (!resolveAttribute(backgroundColorAttrId, typedValue, true)) {
        error("Couldn't resolve attribute ($colorAttrId)")
    }
    val backgroundColor = typedValue.data

    val red = colorPercentage * Color.red(color) + backgroundPercentage * Color.red(backgroundColor)
    val green = colorPercentage * Color.green(color) + backgroundPercentage * Color.green(backgroundColor)
    val blue = colorPercentage * Color.blue(color) + backgroundPercentage * Color.blue(backgroundColor)

    return Color.rgb(red.toInt(), green.toInt(), blue.toInt())
}

fun Theme.getIntArray(attrId: Int): IntArray {
    val typedValue = TypedValue()

    val found = resolveAttribute(attrId, typedValue, true)
    if (!found) {
        throw IllegalStateException("Couldn't resolve attribute ($attrId)")
    }

    return resources.getIntArray(typedValue.resourceId)
}

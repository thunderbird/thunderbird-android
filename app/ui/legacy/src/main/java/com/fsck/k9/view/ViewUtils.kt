package com.fsck.k9.view

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

private const val FADE_DURATION = 300L
private const val ALPHA_VISIBLE = 1.0f
private const val ALPHA_GONE = 0.0f

internal fun View.makeVisible() {
    visibility = View.VISIBLE
}

internal fun View.makeGone() {
    visibility = View.GONE
}

internal fun View.fadeOut() {
    animate()
        .alpha(ALPHA_GONE)
        .setDuration(FADE_DURATION)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .withEndAction {
            makeGone()
        }
}

internal fun View.fadeIn() {
    animate()
        .alpha(ALPHA_VISIBLE)
        .setDuration(FADE_DURATION)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .withEndAction {
            makeVisible()
        }
}

package com.fsck.k9.ui.account

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons

/**
 * Provides a [Drawable] for the account using the account's color as background color.
 */
class AccountFallbackImageProvider(private val context: Context) {
    fun getDrawable(color: Int): Drawable {
        val drawable = ContextCompat.getDrawable(context, Icons.Outlined.Person)
            ?: error("Error loading drawable")

        return drawable.mutate().apply {
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.DST_OVER)
        }
    }
}

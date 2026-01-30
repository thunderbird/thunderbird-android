package com.fsck.k9.contacts

import android.content.Context
import android.view.ContextThemeWrapper
import com.fsck.k9.ui.R
import com.fsck.k9.ui.getIntArray
import com.fsck.k9.ui.resolveColorAttribute
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager
import net.thunderbird.core.ui.theme.manager.ThemeManager

class ContactLetterBitmapConfig(
    context: Context,
    themeManager: ThemeManager,
    messageListPreferencesManager: MessageListPreferencesManager,
) {
    val hasDefaultBackgroundColor: Boolean = !messageListPreferencesManager.getConfig().isColorizeMissingContactPictures
    val defaultBackgroundColor: Int
    val backgroundColors: IntArray

    init {
        val themedContext = ContextThemeWrapper(context, themeManager.appThemeResourceId)
        val theme = themedContext.theme

        if (hasDefaultBackgroundColor) {
            defaultBackgroundColor = theme.resolveColorAttribute(R.attr.contactPictureFallbackDefaultBackgroundColor)
            backgroundColors = intArrayOf()
        } else {
            defaultBackgroundColor = 0
            backgroundColors = theme.getIntArray(R.attr.contactPictureFallbackBackgroundColors)
        }
    }
}

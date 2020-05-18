package com.fsck.k9.contacts

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import com.fsck.k9.K9
import com.fsck.k9.ui.R
import com.fsck.k9.ui.Theme
import com.fsck.k9.ui.ThemeManager

class ContactLetterBitmapConfig(context: Context, themeManager: ThemeManager) {
    val hasDefaultBackgroundColor: Boolean = !K9.isColorizeMissingContactPictures
    val useDarkTheme = themeManager.appTheme == Theme.DARK
    val defaultBackgroundColor: Int

    init {
        defaultBackgroundColor = if (hasDefaultBackgroundColor) {
            val outValue = TypedValue()
            val themedContext = ContextThemeWrapper(context, themeManager.appThemeResourceId)
            themedContext.theme.resolveAttribute(R.attr.contactPictureFallbackDefaultBackgroundColor, outValue, true)
            outValue.data
        } else {
            0
        }
    }
}

package com.fsck.k9.contacts

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import com.fsck.k9.K9
import com.fsck.k9.activity.K9ActivityCommon
import com.fsck.k9.ui.R

class ContactLetterBitmapConfig(context: Context) {
    val hasDefaultBackgroundColor: Boolean = !K9.isColorizeMissingContactPictures()
    val defaultBackgroundColor: Int

    init {
        defaultBackgroundColor = if (hasDefaultBackgroundColor) {
            val outValue = TypedValue()
            val themedContext = ContextThemeWrapper(context, K9ActivityCommon.getK9ThemeResourceId())
            themedContext.theme.resolveAttribute(R.attr.contactPictureFallbackDefaultBackgroundColor, outValue, true)
            outValue.data
        } else {
            0
        }
    }
}

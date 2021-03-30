package com.fsck.k9.ui.account

import android.content.Context
import android.graphics.drawable.Drawable
import com.fsck.k9.ui.R
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.utils.backgroundColorInt
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp

/**
 * Provides a [Drawable] for the account using the account's color as background color.
 */
class AccountFallbackImageProvider(private val context: Context) {
    fun getDrawable(color: Int): Drawable {
        return IconicsDrawable(context, FontAwesome.Icon.faw_user_alt).apply {
            colorRes = R.color.material_drawer_profile_icon
            backgroundColorInt = color
            sizeDp = 56
            paddingDp = 12
        }
    }
}

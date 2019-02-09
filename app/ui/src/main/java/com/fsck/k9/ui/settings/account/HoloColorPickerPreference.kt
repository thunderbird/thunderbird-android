package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.res.TypedArrayUtils
import android.util.AttributeSet
import com.takisoft.fix.support.v7.preference.ColorPickerPreference
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat

@SuppressLint("RestrictedApi")
class HoloColorPickerPreference
@JvmOverloads
constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = TypedArrayUtils.getAttr(context, androidx.preference.R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle),
        defStyleRes: Int = 0
) : ColorPickerPreference(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        init {
            PreferenceFragmentCompat.registerPreferenceFragment(
                    HoloColorPickerPreference::class.java, HoloColorPickerDialogFragment::class.java)
        }
    }
}

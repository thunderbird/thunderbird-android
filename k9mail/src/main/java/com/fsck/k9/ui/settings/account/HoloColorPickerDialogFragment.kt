package com.fsck.k9.ui.settings.account

import android.app.Dialog
import android.os.Bundle
import android.support.v7.preference.PreferenceDialogFragmentCompat
import com.fsck.k9.activity.ColorPickerDialog

class HoloColorPickerDialogFragment : PreferenceDialogFragmentCompat() {
    private val holoColorPickerPreference: HoloColorPickerPreference
        get() = preference as HoloColorPickerPreference


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return ColorPickerDialog(
                context,
                ColorPickerDialog.OnColorChangedListener { color -> updateColor(color) },
                holoColorPickerPreference.color
        )
    }

    override fun onDialogClosed(positiveResult: Boolean) = Unit

    private fun updateColor(pickedColor: Int) {
        val preference = holoColorPickerPreference
        if (preference.callChangeListener(pickedColor)) {
            preference.color = pickedColor
        }
    }
}

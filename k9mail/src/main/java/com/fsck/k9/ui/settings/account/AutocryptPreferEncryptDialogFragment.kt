package com.fsck.k9.ui.settings.account

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.preference.DialogPreference
import com.fsck.k9.ui.dialog.AutocryptPreferEncryptDialog

class AutocryptPreferEncryptDialogFragment : DialogFragment() {
    private val preference: AutocryptPreferEncryptPreference by lazy {
        val preferenceKey = arguments?.getString(ARG_KEY) ?: throw IllegalStateException("Argument $ARG_KEY missing")
        val fragment = targetFragment as DialogPreference.TargetFragment

        fragment.findPreference(preferenceKey) as AutocryptPreferEncryptPreference
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val autocryptPreferEncryptMutual = preference.isChecked

        return AutocryptPreferEncryptDialog(context, autocryptPreferEncryptMutual) { newValue ->
            preference.userChangedValue(newValue)
        }
    }


    companion object {
        private const val ARG_KEY = "key"
    }
}

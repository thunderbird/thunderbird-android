package com.fsck.k9.ui.settings.account

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import com.fsck.k9.ui.R

class AutocryptPreferEncryptDialogFragment : PreferenceDialogFragmentCompat() {
    private val preferEncryptPreference: AutocryptPreferEncryptPreference
        get() = preference as AutocryptPreferEncryptPreference

    private lateinit var preferEncryptCheckbox: CheckBox

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_autocrypt_prefer_encrypt, null)

        view.findViewById<TextView>(R.id.prefer_encrypt_learn_more).makeLinksClickable()

        preferEncryptCheckbox = view.findViewById<CheckBox>(R.id.prefer_encrypt_check).apply {
            isChecked = preferEncryptPreference.isPreferEncryptEnabled
        }

        view.findViewById<View>(R.id.prefer_encrypt).setOnClickListener {
            preferEncryptCheckbox.performClick()
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton(R.string.okay_action, ::onClick)
            .setNegativeButton(R.string.cancel_action, ::onClick)
            .create()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            preferEncryptPreference.setPreferEncryptEnabled(preferEncryptCheckbox.isChecked)
        }
    }

    private fun TextView.makeLinksClickable() {
        this.movementMethod = LinkMovementMethod.getInstance()
    }
}

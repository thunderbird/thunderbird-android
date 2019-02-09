package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import androidx.preference.DialogPreference
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.fsck.k9.ui.R

class AutocryptPreferEncryptDialogFragment : DialogFragment() {
    private val preference: AutocryptPreferEncryptPreference by lazy {
        val preferenceKey = arguments?.getString(ARG_KEY) ?: throw IllegalStateException("Argument $ARG_KEY missing")
        val fragment = targetFragment as DialogPreference.TargetFragment

        fragment.findPreference(preferenceKey) as AutocryptPreferEncryptPreference
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_autocrypt_prefer_encrypt, null)

        view.findViewById<TextView>(R.id.prefer_encrypt_learn_more).makeLinksClickable()

        val preferEncryptCheckbox = view.findViewById<CheckBox>(R.id.prefer_encrypt_check).apply {
            isChecked = preference.isChecked
        }

        view.findViewById<View>(R.id.prefer_encrypt).setOnClickListener {
            preferEncryptCheckbox.performClick()
            preference.userChangedValue(preferEncryptCheckbox.isChecked)
        }

        return AlertDialog.Builder(requireContext())
                // TODO add autocrypt logo?
                //.setIcon(R.drawable.autocrypt)
                .setView(view)
                .setPositiveButton(R.string.done_action, null)
                .create()
    }

    private fun TextView.makeLinksClickable() {
        this.movementMethod = LinkMovementMethod.getInstance()
    }


    companion object {
        private const val ARG_KEY = "key"
    }
}

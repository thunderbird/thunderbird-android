package com.fsck.k9.ui.settings

import androidx.preference.ListPreference
import androidx.preference.Preference

inline fun Preference.onClick(crossinline action: () -> Unit) = setOnPreferenceClickListener {
    action()
    true
}

fun Preference?.remove() = this?.parent?.removePreference(this)

fun ListPreference.removeEntry(entryValue: String) {
    val deleteIndex = entryValues.indexOf(entryValue)
    entries = entries.filterIndexed { index, _ -> index != deleteIndex }.toTypedArray()
    entryValues = entryValues.filterIndexed { index, _ -> index != deleteIndex }.toTypedArray()
}

inline fun Preference.oneTimeClickListener(clickHandled: Boolean = true, crossinline block: () -> Unit) {
    onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
        preference.onPreferenceClickListener = null
        block()
        clickHandled
    }
}

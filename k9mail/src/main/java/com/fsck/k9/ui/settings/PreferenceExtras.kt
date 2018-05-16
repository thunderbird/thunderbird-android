package com.fsck.k9.ui.settings

import android.support.v7.preference.Preference


inline fun Preference.onClick(crossinline action: () -> Unit) = setOnPreferenceClickListener {
    action()
    true
}

fun Preference.remove() = parent?.removePreference(this)

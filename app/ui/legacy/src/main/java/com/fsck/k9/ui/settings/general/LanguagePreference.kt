package com.fsck.k9.ui.settings.general

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.ListPreference
import com.fsck.k9.core.R

class LanguagePreference
@JvmOverloads
@SuppressLint("RestrictedApi")
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(
        context,
        androidx.preference.R.attr.dialogPreferenceStyle,
        android.R.attr.dialogPreferenceStyle,
    ),
    defStyleRes: Int = 0,
) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        val supportedLanguages = context.resources.getStringArray(R.array.supported_languages).toSet()

        val newEntries = mutableListOf<CharSequence>()
        val newEntryValues = mutableListOf<CharSequence>()
        entryValues.forEachIndexed { index, language ->
            if (language in supportedLanguages) {
                newEntries.add(entries[index])
                newEntryValues.add(entryValues[index])
            }
        }

        entries = newEntries.toTypedArray()
        entryValues = newEntryValues.toTypedArray()
    }
}

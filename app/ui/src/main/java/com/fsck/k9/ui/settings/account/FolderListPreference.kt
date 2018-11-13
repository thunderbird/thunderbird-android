package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.res.TypedArrayUtils
import android.support.v7.preference.ListPreference
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.AttributeSet
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.ui.R
import com.fsck.k9.ui.folders.FolderNameFormatter
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * A [ListPreference] that allows selecting one of an account's folders.
 */
@SuppressLint("RestrictedApi")
class FolderListPreference
@JvmOverloads
constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = TypedArrayUtils.getAttr(context, android.support.v7.preference.R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle),
        defStyleRes: Int = 0
) : ListPreference(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
    private val folderNameFormatter: FolderNameFormatter by inject()
    private val noFolderSelectedName = SpannableString(context.getString(R.string.account_settings_no_folder_selected))
            .apply { setSpan(StyleSpan(Typeface.ITALIC), 0, this.length, 0) }


    var folders: List<Folder>
        get() = throw UnsupportedOperationException()
        set(folders) {
            entries = (listOf(noFolderSelectedName) + folders.map { folderNameFormatter.displayName(it) }).toTypedArray()
            entryValues = (listOf(NO_FOLDER_SELECTED_VALUE) + folders.map { it.serverId }).toTypedArray()

            isEnabled = true
        }


    init {
        entries = emptyArray()
        entryValues = emptyArray()
        isEnabled = false
    }

    override fun getSummary(): CharSequence {
        // While folders are being loaded the summary returned by ListPreference will be empty. This leads to the
        // summary view being hidden. Once folders are loaded the summary updates and the list height changes. This
        // adds quite a bit of visual clutter. We avoid that by returning a placeholder summary value.
        return when {
            entries.isEmpty() -> PLACEHOLDER_SUMMARY
            value == NO_FOLDER_SELECTED_VALUE -> noFolderSelectedName
            else -> super.getSummary()
        }
    }


    companion object {
        const val NO_FOLDER_SELECTED_VALUE = ""
        private const val PLACEHOLDER_SUMMARY = " "
    }
}

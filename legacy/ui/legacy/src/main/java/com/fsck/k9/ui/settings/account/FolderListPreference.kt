package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.ListPreference
import app.k9mail.legacy.folder.RemoteFolder
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import com.fsck.k9.ui.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * A [ListPreference] that allows selecting one of an account's folders.
 */
@SuppressLint("RestrictedApi")
class FolderListPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(
        context,
        androidx.preference.R.attr.dialogPreferenceStyle,
        android.R.attr.dialogPreferenceStyle,
    ),
    defStyleRes: Int = 0,
) : ListPreference(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
    private val folderNameFormatter: FolderNameFormatter by inject { parametersOf(context) }
    private val noFolderSelectedName = context.getString(R.string.account_settings_no_folder_selected).italicize()
    private lateinit var automaticFolderOption: CharSequence

    init {
        entries = emptyArray()
        entryValues = emptyArray()
        isEnabled = false
    }

    fun setFolders(folders: List<RemoteFolder>) {
        entries = (listOf(noFolderSelectedName) + getFolderDisplayNames(folders)).toTypedArray()
        entryValues = (listOf(NO_FOLDER_SELECTED_VALUE) + getFolderValues(folders)).toTypedArray()
        isEnabled = true
    }

    fun setFolders(folders: List<RemoteFolder>, automaticFolder: RemoteFolder?) {
        val automaticFolderName = if (automaticFolder != null) {
            folderNameFormatter.displayName(automaticFolder)
        } else {
            context.getString(R.string.account_settings_no_folder_selected)
        }
        val automaticFolderValue = AUTOMATIC_PREFIX + (automaticFolder?.id?.toString() ?: NO_FOLDER_VALUE)

        automaticFolderOption = context.getString(
            R.string.account_settings_automatic_special_folder,
            automaticFolderName,
        ).italicize()

        entries = (listOf(automaticFolderOption) + noFolderSelectedName + getFolderDisplayNames(folders)).toTypedArray()
        entryValues =
            (listOf(automaticFolderValue) + NO_FOLDER_SELECTED_VALUE + getFolderValues(folders)).toTypedArray()

        isEnabled = true
    }

    override fun getSummary(): CharSequence? {
        // While folders are being loaded the summary returned by ListPreference will be empty. This leads to the
        // summary view being hidden. Once folders are loaded the summary updates and the list height changes. This
        // adds quite a bit of visual clutter. We avoid that by returning a placeholder summary value.
        return when {
            entries.isEmpty() -> PLACEHOLDER_SUMMARY
            value == NO_FOLDER_SELECTED_VALUE -> noFolderSelectedName
            value.startsWith(AUTOMATIC_PREFIX) -> automaticFolderOption
            else -> super.getSummary()
        }
    }

    private fun getFolderDisplayNames(folders: List<RemoteFolder>) = folders.map { folderNameFormatter.displayName(it) }

    private fun getFolderValues(folders: List<RemoteFolder>) = folders.map { MANUAL_PREFIX + it.id.toString() }

    private fun String.italicize(): CharSequence {
        return SpannableString(this).apply { setSpan(StyleSpan(Typeface.ITALIC), 0, this.length, 0) }
    }

    companion object {
        const val FOLDER_VALUE_DELIMITER = "|"
        const val AUTOMATIC_PREFIX = "AUTOMATIC|"
        const val MANUAL_PREFIX = "MANUAL|"
        const val NO_FOLDER_VALUE = ""
        private const val NO_FOLDER_SELECTED_VALUE = MANUAL_PREFIX + NO_FOLDER_VALUE
        private const val PLACEHOLDER_SUMMARY = " "
    }
}

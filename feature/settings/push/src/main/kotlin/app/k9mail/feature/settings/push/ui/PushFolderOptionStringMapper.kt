package app.k9mail.feature.settings.push.ui

import android.content.res.Resources
import app.k9mail.feature.settings.push.R
import com.fsck.k9.Account.FolderMode

internal fun FolderMode.toResourceString(resources: Resources): String {
    val resourceId = when (this) {
        FolderMode.NONE -> R.string.settings_push_option_none
        FolderMode.ALL -> R.string.settings_push_option_all
        FolderMode.FIRST_CLASS -> R.string.settings_push_option_first_class
        FolderMode.FIRST_AND_SECOND_CLASS -> R.string.settings_push_option_first_and_second_class
        FolderMode.NOT_SECOND_CLASS -> R.string.settings_push_option_all_except_second_class
    }

    return resources.getString(resourceId)
}

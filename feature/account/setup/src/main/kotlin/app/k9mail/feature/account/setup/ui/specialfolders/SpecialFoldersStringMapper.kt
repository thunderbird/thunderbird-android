package app.k9mail.feature.account.setup.ui.specialfolders

import android.content.res.Resources
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.setup.R

internal fun SpecialFolderOption.toResourceString(resources: Resources) = when (this) {
    is SpecialFolderOption.None -> {
        val noneString = resources.getString(R.string.account_setup_special_folders_folder_none)
        if (isAutomatic) {
            resources.getString(R.string.account_setup_special_folders_folder_automatic, noneString)
        } else {
            noneString
        }
    }
    is SpecialFolderOption.Regular -> remoteFolder.displayName
    is SpecialFolderOption.Special -> {
        if (isAutomatic) {
            resources.getString(R.string.account_setup_special_folders_folder_automatic, remoteFolder.displayName)
        } else {
            remoteFolder.displayName
        }
    }
}

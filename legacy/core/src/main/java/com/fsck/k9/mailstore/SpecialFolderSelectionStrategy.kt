package com.fsck.k9.mailstore

import net.thunderbird.feature.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.FolderType

/**
 * Implements the automatic special folder selection strategy.
 */
class SpecialFolderSelectionStrategy {
    fun selectSpecialFolder(
        folders: List<RemoteFolder>,
        type: FolderType,
    ): RemoteFolder? {
        return folders.firstOrNull { folder -> folder.type == type }
    }
}

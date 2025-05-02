package com.fsck.k9.mailstore

import app.k9mail.core.mail.folder.api.FolderType
import net.thunderbird.feature.folder.api.RemoteFolder

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

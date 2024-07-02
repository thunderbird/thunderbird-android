package com.fsck.k9.mailstore

/**
 * Implements the automatic special folder selection strategy.
 */
class SpecialFolderSelectionStrategy {
    fun selectSpecialFolder(folders: List<RemoteFolder>, type: FolderType): RemoteFolder? {
        return folders.firstOrNull { folder -> folder.type == type }
    }
}

package com.fsck.k9.mailstore

interface BackendFoldersRefreshListener {
    fun onBeforeFolderListRefresh()
    fun onAfterFolderListRefresh()
}

package app.k9mail.feature.navigation.drawer.legacy

import app.k9mail.legacy.ui.folder.DisplayFolder

internal data class FolderList(
    val unifiedInbox: DisplayUnifiedInbox?,
    val accountId: Int,
    val folders: List<DisplayFolder>,
)

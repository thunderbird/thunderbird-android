package app.k9mail.feature.navigation.drawer.ui.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItem
import app.k9mail.feature.navigation.drawer.R
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolderType

@Composable
fun FolderListItem(
    displayFolder: DisplayFolder,
    selected: Boolean,
    onClick: (DisplayFolder) -> Unit,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItem(
        label = mapFolderName(displayFolder),
        selected = selected,
        onClick = { onClick(displayFolder) },
        modifier = modifier,
        icon = {
            Icon(
                imageVector = mapFolderIcon(displayFolder),
            )
        },
        badge = {
            FolderListItemBadge(
                unreadCount = displayFolder.unreadMessageCount,
                starredCount = displayFolder.starredMessageCount,
                showStarredCount = showStarredCount,
            )
        },
    )
}

@Composable
private fun mapFolderName(folder: DisplayFolder): String {
    return when (folder) {
        is DisplayAccountFolder -> folder.folder.name
        is DisplayUnifiedFolder -> mapUnifiedFolderName(folder)
        else -> throw IllegalArgumentException("Unknown display folder type: $folder")
    }
}

@Composable
private fun mapUnifiedFolderName(folder: DisplayUnifiedFolder): String {
    return when (folder.unifiedType) {
        DisplayUnifiedFolderType.INBOX -> stringResource(R.string.navigation_drawer_unified_inbox_title)
    }
}

private fun mapFolderIcon(folder: DisplayFolder): ImageVector {
    return when (folder) {
        is DisplayAccountFolder -> mapDisplayAccountFolderIcon(folder)
        is DisplayUnifiedFolder -> mapDisplayUnifiedFolderIcon(folder)
        else -> throw IllegalArgumentException("Unknown display folder type: $folder")
    }
}

private fun mapDisplayAccountFolderIcon(folder: DisplayAccountFolder): ImageVector {
    return when (folder.folder.type) {
        FolderType.INBOX -> Icons.Outlined.Inbox
        FolderType.OUTBOX -> Icons.Outlined.Outbox
        FolderType.SENT -> Icons.Outlined.Send
        FolderType.TRASH -> Icons.Outlined.Delete
        FolderType.DRAFTS -> Icons.Outlined.Drafts
        FolderType.ARCHIVE -> Icons.Outlined.Archive
        FolderType.SPAM -> Icons.Outlined.Report
        FolderType.REGULAR -> Icons.Outlined.Folder
    }
}

private fun mapDisplayUnifiedFolderIcon(folder: DisplayUnifiedFolder): ImageVector {
    when (folder.unifiedType) {
        DisplayUnifiedFolderType.INBOX -> return Icons.Outlined.AllInbox
    }
}

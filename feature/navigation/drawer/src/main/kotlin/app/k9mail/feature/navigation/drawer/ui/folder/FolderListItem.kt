package app.k9mail.feature.navigation.drawer.ui.folder

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItem
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.R
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolderType
import app.k9mail.feature.navigation.drawer.domain.entity.TreeFolder
import app.k9mail.legacy.ui.folder.FolderNameFormatter

@Composable
internal fun FolderListItem(
    displayFolder: DisplayFolder,
    selected: Boolean,
    onClick: (DisplayFolder) -> Unit,
    showStarredCount: Boolean,
    folderNameFormatter: FolderNameFormatter,
    modifier: Modifier = Modifier,
    treeFolder: TreeFolder? = null,
    parentPrefix: String? = "",
) {
    var isExpanded = remember { mutableStateOf(false) }

    var unreadCount = displayFolder.unreadMessageCount
    var starredCount = displayFolder.starredMessageCount

    if (treeFolder !== null && !isExpanded.value) {
        unreadCount = treeFolder.getAllUnreadMessageCount()
        starredCount = treeFolder.getAllStarredMessageCount()
    }

    NavigationDrawerItem(
        label = mapFolderName(displayFolder, folderNameFormatter, parentPrefix),
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
                unreadCount = unreadCount,
                starredCount = starredCount,
                showStarredCount = showStarredCount,
                expandableState = if (treeFolder !== null && treeFolder.children.isNotEmpty()) isExpanded else null,
            )
        },
    )

    // Managing children
    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        if (!isExpanded.value) return
        if (treeFolder === null) return
        for (child in treeFolder.children) {
            var displayParent = treeFolder.value
            var displayChild = child.value
            if (displayChild === null) continue
            FolderListItem(
                displayFolder = displayChild,
                selected = false,
                showStarredCount = showStarredCount,
                onClick = { onClick(displayChild) },
                folderNameFormatter = folderNameFormatter,
                modifier = modifier.then(Modifier.padding(start = MainTheme.spacings.triple)),
                treeFolder = child,
                parentPrefix = if (displayParent is DisplayAccountFolder) displayParent.folder.name else null,
            )
        }
    }
}

@Composable
private fun mapFolderName(
    displayFolder: DisplayFolder,
    folderNameFormatter: FolderNameFormatter,
    parentPrefix: String? = "",
): String {
    return when (displayFolder) {
        is DisplayAccountFolder -> folderNameFormatter.displayName(displayFolder.folder).removePrefix("$parentPrefix/")
        is DisplayUnifiedFolder -> mapUnifiedFolderName(displayFolder)
        else -> throw IllegalArgumentException("Unknown display folder: $displayFolder")
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

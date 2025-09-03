package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItem
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.AnimatedExpandIcon

@Composable
internal fun FolderListItem(
    displayFolder: DisplayFolder,
    onClick: (DisplayFolder) -> Unit,
    showStarredCount: Boolean,
    folderNameFormatter: FolderNameFormatter,
    selectedFolderId: String?,
    modifier: Modifier = Modifier,
    treeFolder: DisplayTreeFolder? = null,
    parentPrefix: String? = "",
    indentationLevel: Int = 1,
) {
    val isExpanded = rememberSaveable { mutableStateOf(false) }

    var unreadCount = displayFolder.unreadMessageCount
    var starredCount = displayFolder.starredMessageCount

    if (treeFolder !== null && !isExpanded.value) {
        unreadCount = treeFolder.totalUnreadCount
        starredCount = treeFolder.totalStarredCount
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        NavigationDrawerItem(
            label = {
                NavigationDrawerLabel(
                    label = mapFolderName(displayFolder, folderNameFormatter, parentPrefix),
                    expandableState = if (treeFolder !== null && treeFolder.children.isNotEmpty()) isExpanded else null,
                    badge = {
                        FolderListItemBadge(
                            unreadCount = unreadCount,
                            starredCount = starredCount,
                            showStarredCount = showStarredCount,
                        )
                    },
                )
            },
            selected = selectedFolderId == displayFolder.id,
            onClick = {
                when (displayFolder) {
                    is MailDisplayFolder if displayFolder.accountId == null -> isExpanded.value = !isExpanded.value
                    else -> onClick(displayFolder)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(imageVector = mapFolderIcon(displayFolder)) },
        )

        // Managing children
        if (!isExpanded.value) return
        if (treeFolder === null) return
        for (child in treeFolder.children) {
            val displayParent = treeFolder.displayFolder
            val displayChild = child.displayFolder
            if (displayChild == null) continue
            FolderListItem(
                displayFolder = displayChild,
                selectedFolderId = selectedFolderId,
                showStarredCount = showStarredCount,
                onClick = onClick,
                folderNameFormatter = folderNameFormatter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = MainTheme.spacings.double * indentationLevel),
                treeFolder = child,
                parentPrefix = if (displayParent is MailDisplayFolder) displayParent.folder.name else null,
                indentationLevel = indentationLevel + 1,
            )
        }
    }
}

@Composable
private fun NavigationDrawerLabel(
    label: String,
    badge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    expandableState: MutableState<Boolean>? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextLabelLarge(
            text = label,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        if (expandableState?.value != null) {
            Box(
                modifier = Modifier
                    .size(MainTheme.sizes.iconAvatar)
                    .padding(
                        start = MainTheme.spacings.quarter,
                        end = MainTheme.spacings.quarter,
                    )
                    .clip(CircleShape)
                    .clickable(onClick = { expandableState.value = !expandableState.value }),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedExpandIcon(
                    isExpanded = expandableState.value,
                )
            }
        }
        badge()
    }
}

@Composable
private fun mapFolderName(
    displayFolder: DisplayFolder,
    folderNameFormatter: FolderNameFormatter,
    parentPrefix: String? = "",
): String {
    return when (displayFolder) {
        is MailDisplayFolder -> folderNameFormatter.displayName(displayFolder.folder).removePrefix("$parentPrefix/")
        is UnifiedDisplayFolder -> mapUnifiedFolderName(displayFolder)
        else -> throw IllegalArgumentException("Unknown display folder: $displayFolder")
    }
}

@Composable
private fun mapUnifiedFolderName(folder: UnifiedDisplayFolder): String {
    return when (folder.unifiedType) {
        UnifiedDisplayFolderType.INBOX -> stringResource(R.string.navigation_drawer_dropdown_unified_inbox_title)
    }
}

private fun mapFolderIcon(folder: DisplayFolder): ImageVector {
    return when (folder) {
        is MailDisplayFolder -> mapDisplayAccountFolderIcon(folder)
        is UnifiedDisplayFolder -> mapDisplayUnifiedFolderIcon(folder)
        else -> throw IllegalArgumentException("Unknown display folder type: $folder")
    }
}

private fun mapDisplayAccountFolderIcon(folder: MailDisplayFolder): ImageVector {
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

private fun mapDisplayUnifiedFolderIcon(folder: UnifiedDisplayFolder): ImageVector {
    when (folder.unifiedType) {
        UnifiedDisplayFolderType.INBOX -> return Icons.Outlined.AllInbox
    }
}

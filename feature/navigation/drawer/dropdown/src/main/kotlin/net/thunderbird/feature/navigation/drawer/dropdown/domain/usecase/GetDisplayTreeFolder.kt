package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccountFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayUnifiedFolder

internal class GetDisplayTreeFolder : UseCase.GetDisplayTreeFolder {
    private var placeholderCounter = 0L

    override fun invoke(folders: List<DisplayFolder>, maxDepth: Int): DisplayTreeFolder {
        val unifiedFolderTreeList = folders.filterIsInstance<DisplayUnifiedFolder>().map {
            DisplayTreeFolder(
                displayFolder = it,
                displayName = it.unifiedType.id,
                totalUnreadCount = it.unreadMessageCount,
                totalStarredCount = it.starredMessageCount,
                children = persistentListOf(),
            )
        }

        val accountFolders = folders.filterIsInstance<DisplayAccountFolder>().map {
            val path = flattenPath(it.folder.name, maxDepth)
            println("Flattened path for ${it.folder.name} → $path")
            path to it
        }
        val accountFolderTreeList = buildAccountFolderTree(accountFolders)

        return DisplayTreeFolder(
            displayFolder = null,
            displayName = null,
            totalUnreadCount = accountFolderTreeList.sumOf { it.totalUnreadCount },
            totalStarredCount = accountFolderTreeList.sumOf { it.totalStarredCount },
            children = (unifiedFolderTreeList + accountFolderTreeList).toImmutableList(),
        )
    }

    private fun flattenPath(folderName: String, maxDepth: Int): List<String> {
        val parts = folderName.split("/").map { it.takeIf { it.isNotBlank() } ?: "(Unnamed)" }

        return if (parts.size <= maxDepth) {
            parts
        } else {
            parts.take(maxDepth) + listOf(parts.drop(maxDepth).joinToString("/"))
        }
    }

    private fun buildAccountFolderTree(
        paths: List<Pair<List<String>, DisplayAccountFolder>>,
        parentPath: String = "",
    ): List<DisplayTreeFolder> {
        return paths.groupBy { it.first.getOrNull(0) ?: "(Unnamed)" }
            .map { (segment, entries) ->
                val childPaths = entries.mapNotNull { (segments, folders) ->
                    if (segments.size > 1) {
                        Pair(segments.drop(1), folders)
                    } else {
                        null
                    }
                }

                val currentFolders = entries.mapNotNull { (segments, folder) ->
                    if (segments.size == 1) folder else null
                }

                val fullPath = if (parentPath.isBlank()) segment else "$parentPath/$segment"

                val currentFolder = currentFolders.firstOrNull() ?: createPlaceholderFolder(fullPath)

                val children = buildAccountFolderTree(childPaths, fullPath)

                val totalUnread = children.sumOf { it.totalUnreadCount } + currentFolder.unreadMessageCount
                val totalStarred = children.sumOf { it.totalStarredCount } + currentFolder.starredMessageCount

                DisplayTreeFolder(
                    displayFolder = currentFolder,
                    displayName = segment,
                    totalUnreadCount = totalUnread,
                    totalStarredCount = totalStarred,
                    children = children.toImmutableList(),
                )
            }
    }

    private fun createPlaceholderFolder(name: String): DisplayAccountFolder {
        placeholderCounter += 1
        return DisplayAccountFolder(
            accountId = "placeholder",
            folder = Folder(
                id = placeholderCounter,
                name = name,
                type = FolderType.REGULAR,
                isLocalOnly = false,
            ),
            isInTopGroup = true,
            unreadMessageCount = 0,
            starredMessageCount = 0,
        )
    }
}

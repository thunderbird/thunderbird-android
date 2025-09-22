package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder

internal class GetDisplayTreeFolder(
    private val logger: Logger,
) : UseCase.GetDisplayTreeFolder {
    private var placeholderCounter = 0L

    override fun invoke(folders: List<DisplayFolder>, maxDepth: Int): DisplayTreeFolder {
        val unifiedFolderTreeList = folders.filterIsInstance<UnifiedDisplayFolder>().map {
            DisplayTreeFolder(
                displayFolder = it,
                displayName = it.unifiedType.id,
                totalUnreadCount = it.unreadMessageCount,
                totalStarredCount = it.starredMessageCount,
                children = persistentListOf(),
            )
        }

        val pathDelimiter = folders.firstOrNull()?.pathDelimiter ?: FOLDER_DEFAULT_PATH_DELIMITER
        val accountFolders = folders.filterIsInstance<MailDisplayFolder>().map {
            val path = flattenPath(it.folder.name, pathDelimiter, maxDepth)
            logger.debug { "Flattened path for ${it.folder.name} → $path" }
            path to it
        }
        val accountFolderTreeList = buildAccountFolderTree(accountFolders, pathDelimiter)

        return DisplayTreeFolder(
            displayFolder = null,
            displayName = null,
            totalUnreadCount = accountFolderTreeList.sumOf { it.totalUnreadCount },
            totalStarredCount = accountFolderTreeList.sumOf { it.totalStarredCount },
            children = (unifiedFolderTreeList + accountFolderTreeList).toImmutableList(),
        )
    }

    private fun flattenPath(folderName: String, folderPathDelimiter: FolderPathDelimiter, maxDepth: Int): List<String> {
        val parts = folderName.split(folderPathDelimiter).map { it.takeIf { it.isNotBlank() } ?: "(Unnamed)" }

        return if (parts.size <= maxDepth) {
            parts
        } else {
            parts.take(maxDepth) + listOf(parts.drop(maxDepth).joinToString(folderPathDelimiter))
        }
    }

    private fun buildAccountFolderTree(
        paths: List<Pair<List<String>, MailDisplayFolder>>,
        pathDelimiter: FolderPathDelimiter,
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

                val fullPath = if (parentPath.isBlank()) segment else "${parentPath}${pathDelimiter}$segment"

                val currentFolder = currentFolders.firstOrNull() ?: createPlaceholderFolder(fullPath, pathDelimiter)

                val children = buildAccountFolderTree(
                    paths = childPaths,
                    pathDelimiter = pathDelimiter,
                    parentPath = fullPath,
                )

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

    private fun createPlaceholderFolder(name: String, pathDelimiter: FolderPathDelimiter): MailDisplayFolder {
        placeholderCounter += 1
        return MailDisplayFolder(
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
            pathDelimiter = pathDelimiter,
        )
    }
}

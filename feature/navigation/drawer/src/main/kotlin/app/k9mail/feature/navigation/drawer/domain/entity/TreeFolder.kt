package app.k9mail.feature.navigation.drawer.domain.entity

import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderType

internal data class TreeFolder(
    var value: DisplayFolder? = null,
) {
    companion object {
        fun createFromFolders(folders: List<DisplayFolder>, maxDepth: Int = 3): TreeFolder {
            // Converting folders to TreeFolder
            val rootFolder = TreeFolder()
            var currentTree = rootFolder

            for (displayFolder in folders) {
                if (displayFolder is DisplayUnifiedFolder) {
                    currentTree.children.add(TreeFolder(displayFolder))
                }
                if (displayFolder !is DisplayAccountFolder) continue
                val splittedFolderName = displayFolder.folder.name.split("/", limit = maxDepth + 1)
                var subFolderEntireName = ""
                for (subFolderName in splittedFolderName) {
                    subFolderEntireName += subFolderName
                    var foundInChildren = false
                    for (children in currentTree.children) {
                        var childDisplayFolder = children.value
                        if (childDisplayFolder !is DisplayAccountFolder) continue
                        if (childDisplayFolder.folder.name == subFolderEntireName) {
                            currentTree = children
                            foundInChildren = true
                            break
                        }
                    }
                    if (!foundInChildren) {
                        var newChildren = TreeFolder()
                        if (subFolderEntireName == displayFolder.folder.name) {
                            newChildren = TreeFolder(displayFolder)
                        } else {
                            newChildren = TreeFolder(
                                DisplayAccountFolder(
                                    displayFolder.accountId,
                                    Folder(0, subFolderEntireName, FolderType.REGULAR, displayFolder.folder.isLocalOnly),
                                    displayFolder.isInTopGroup,
                                    0,
                                    0,
                                ),
                            )
                        }
                        currentTree.children.add(newChildren)
                        currentTree = newChildren
                    } else {
                        if (subFolderEntireName == displayFolder.folder.name) {
                            currentTree.value = displayFolder
                        }
                    }
                    subFolderEntireName += "/"
                }
                currentTree = rootFolder
            }

            return rootFolder
        }
    }

    val children: ArrayList<TreeFolder> = ArrayList()

    fun getAllUnreadMessageCount(): Int {
        var allUnreadMessageCount = 0
        for (child in children) {
            allUnreadMessageCount += child.getAllUnreadMessageCount()
        }
        return allUnreadMessageCount + (value?.unreadMessageCount ?: 0)
    }

    fun getAllStarredMessageCount(): Int {
        var allStarredMessageCount = 0
        for (child in children) {
            allStarredMessageCount += child.getAllStarredMessageCount()
        }
        return allStarredMessageCount + (value?.starredMessageCount ?: 0)
    }
}

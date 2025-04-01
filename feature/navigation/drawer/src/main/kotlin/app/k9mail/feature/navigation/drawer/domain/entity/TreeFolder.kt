package app.k9mail.feature.navigation.drawer.domain.entity

import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderType

internal data class TreeFolder(
    var value: DisplayFolder? = null
) {
    companion object {
        fun createFromFolders(folders: List<DisplayFolder>, maxDepth: Int = 3): TreeFolder {
            // Preparing root
            val rootFolder = TreeFolder()
            var currentTree = rootFolder

            for (displayFolder in folders) {
                // Managing exceptions
                if (displayFolder is DisplayUnifiedFolder) {
                    currentTree.children.add(TreeFolder(displayFolder))
                }
                if (displayFolder !is DisplayAccountFolder) continue

                val splittedFolderName = displayFolder.folder.name.split("/", limit = maxDepth + 1)
                var currentWorkingPath = ""

                for (subFolderName in splittedFolderName) {
                    currentWorkingPath += subFolderName
                    var foundInChildren = false

                    // finding subFolderEntireName (current working path) into currentTree children
                    for (children in currentTree.children) {
                        var childDisplayFolder = children.value
                        if (childDisplayFolder !is DisplayAccountFolder) continue
                        if (childDisplayFolder.folder.name == currentWorkingPath) {
                            currentTree = children
                            foundInChildren = true
                            break
                        }
                    }

                    // if not found in children, creating a new one
                    if (!foundInChildren) {
                        var newChildren = TreeFolder()
                        if (currentWorkingPath == displayFolder.folder.name) {
                            // if it is the final subfolder, adding displayFolder in it
                            newChildren = TreeFolder(displayFolder)
                        } else {
                            // if just an intermediate, adding a fake subFolder
                            newChildren = TreeFolder(
                                DisplayAccountFolder(
                                    displayFolder.accountId,
                                    Folder(0, currentWorkingPath, FolderType.REGULAR, displayFolder.folder.isLocalOnly),
                                    displayFolder.isInTopGroup,
                                    0,
                                    0,
                                ),
                            )
                        }
                        currentTree.children.add(newChildren)
                        currentTree = newChildren
                    } else {
                        // if found, association the value to manage fake created subFolders
                        if (currentWorkingPath == displayFolder.folder.name) {
                            currentTree.value = displayFolder
                        }
                    }
                    currentWorkingPath += "/"
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

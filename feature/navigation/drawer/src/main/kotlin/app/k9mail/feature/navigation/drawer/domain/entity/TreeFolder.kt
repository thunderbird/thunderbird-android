package app.k9mail.feature.navigation.drawer.domain.entity

import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderType

internal class TreeFolder(
    var displayFolder: DisplayFolder? = null,
) {
    companion object {
        fun createFromFolders(folders: List<DisplayFolder>, maxDepth: Int = 3): TreeFolder {
            // Preparing root
            val rootFolder = TreeFolder()

            for (displayFolder in folders) {
                // Managing exceptions
                if (displayFolder is DisplayUnifiedFolder) {
                    rootFolder.children.add(TreeFolder(displayFolder))
                }
                if (displayFolder !is DisplayAccountFolder) continue

                // Inserting folder in tree
                insertFolderInCurrentTree(rootFolder, displayFolder, maxDepth)
            }

            return rootFolder
        }

        private fun insertFolderInCurrentTree(
            rootTree: TreeFolder,
            displayFolder: DisplayAccountFolder,
            maxDepth: Int,
        ) {
            val splittedFolderName = displayFolder.folder.name.split("/", limit = maxDepth + 1)
            var currentWorkingPath = ""
            var currentTree = rootTree

            for (subFolderName in splittedFolderName) {
                currentWorkingPath += subFolderName

                // finding subFolderEntireName (current working path) into currentTree children
                val foundChild = currentTree.children.find { child ->
                    val childDisplayFolder = child.displayFolder
                    childDisplayFolder is DisplayAccountFolder && childDisplayFolder.folder.name == currentWorkingPath
                }

                if (foundChild != null) {
                    // if found, association the value to manage fake created subFolders
                    currentTree = foundChild
                    if (currentWorkingPath == displayFolder.folder.name) {
                        currentTree.displayFolder = displayFolder
                    }
                } else {
                    // if not found in children, creating a new one
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
                }

                currentWorkingPath += "/"
            }
        }
    }

    val children: ArrayList<TreeFolder> = ArrayList()

    fun getAllUnreadMessageCount(): Int {
        var allUnreadMessageCount = 0
        for (child in children) {
            allUnreadMessageCount += child.getAllUnreadMessageCount()
        }
        return allUnreadMessageCount + (displayFolder?.unreadMessageCount ?: 0)
    }

    fun getAllStarredMessageCount(): Int {
        var allStarredMessageCount = 0
        for (child in children) {
            allStarredMessageCount += child.getAllStarredMessageCount()
        }
        return allStarredMessageCount + (displayFolder?.starredMessageCount ?: 0)
    }
}

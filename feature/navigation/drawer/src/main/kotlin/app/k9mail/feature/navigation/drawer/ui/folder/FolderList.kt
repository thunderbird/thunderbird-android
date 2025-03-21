package app.k9mail.feature.navigation.drawer.ui.folder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolder
import app.k9mail.feature.navigation.drawer.domain.entity.TreeFolder
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun FolderList(
    folders: ImmutableList<DisplayFolder>,
    selectedFolder: DisplayFolder?,
    onFolderClick: (DisplayFolder) -> Unit,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val folderNameFormatter = remember { FolderNameFormatter(resources) }
    val listState = rememberLazyListState()

    // Converting folders to TreeFolder
    val rootFolder = TreeFolder()
    val maxDepth = 2
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

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(vertical = MainTheme.spacings.default),
    ) {
        items(
            items = rootFolder.children,
            key = { it.value?.id ?: '0' },
        ) { folder ->
            val currentDisplayFolder = folder.value
            if (currentDisplayFolder is DisplayAccountFolder) {
                FolderListItem(
                    displayFolder = currentDisplayFolder,
                    selected = currentDisplayFolder.folder == selectedFolder,
                    showStarredCount = showStarredCount,
                    onClick = onFolderClick,
                    folderNameFormatter = folderNameFormatter,
                    treeFolder = folder,
                )
            }
            if (currentDisplayFolder is DisplayUnifiedFolder) {
                DividerHorizontal(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = MainTheme.spacings.default,
                            horizontal = MainTheme.spacings.triple,
                        ),
                )
            }
        }
    }
}

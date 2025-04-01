package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

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
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccountFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayUnifiedFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.TreeFolder

@Composable
internal fun FolderList(
    rootFolder: TreeFolder,
    selectedFolder: DisplayFolder?,
    onFolderClick: (DisplayFolder) -> Unit,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val folderNameFormatter = remember { FolderNameFormatter(resources) }
    val listState = rememberLazyListState()

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

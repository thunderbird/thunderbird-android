package app.k9mail.feature.navigation.drawer.ui.folder

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import kotlinx.collections.immutable.ImmutableList

@Composable
fun FolderList(
    folders: ImmutableList<DisplayAccountFolder>,
    selectedFolder: DisplayAccountFolder?,
    onFolderClick: (DisplayAccountFolder) -> Unit,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
    ) {
        items(folders) { folder ->
            FolderListItem(
                displayFolder = folder,
                selected = folder == selectedFolder,
                showStarredCount = showStarredCount,
                onClick = onFolderClick,
            )
        }
    }
}

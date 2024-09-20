package app.k9mail.feature.navigation.drawer.ui.folder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolder
import kotlinx.collections.immutable.ImmutableList

@Composable
fun FolderList(
    folders: ImmutableList<DisplayFolder>,
    selectedFolder: DisplayFolder?,
    onFolderClick: (DisplayFolder) -> Unit,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(vertical = MainTheme.spacings.default),
    ) {
        items(
            items = folders,
            key = { it.id },
        ) { folder ->
            FolderListItem(
                displayFolder = folder,
                selected = folder == selectedFolder,
                showStarredCount = showStarredCount,
                onClick = onFolderClick,
            )
            if (folder is DisplayUnifiedFolder) {
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

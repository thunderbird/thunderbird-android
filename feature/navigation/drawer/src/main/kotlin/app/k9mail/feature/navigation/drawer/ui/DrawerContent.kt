package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItem
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
fun DrawerContent(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("DrawerContent"),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = MainTheme.spacings.oneHalf,
                ),
        ) {
            item {
                NavigationDrawerItem(
                    label = "Folder1",
                    selected = true,
                    onClick = {},
                )
            }
            item {
                NavigationDrawerItem(
                    label = "Folder2",
                    selected = false,
                    onClick = {},
                )
            }
            item {
                NavigationDrawerItem(
                    label = "Folder3",
                    selected = false,
                    onClick = {},
                )
            }
        }
    }
}

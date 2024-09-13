package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.feature.navigation.drawer.ui.account.AccountView
import app.k9mail.feature.navigation.drawer.ui.folder.FolderList

@Composable
fun DrawerContent(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("DrawerContent"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = MainTheme.spacings.oneHalf,
                ),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            state.currentAccount?.let {
                AccountView(
                    displayName = it.account.displayName,
                    emailAddress = it.account.email,
                    accountColor = it.account.chipColor,
                    onClick = { onEvent(Event.OnAccountViewClick(it)) },
                )

                DividerHorizontal()
            }
            FolderList(
                folders = state.folders,
                selectedFolder = state.folders.firstOrNull(), // TODO Use selected folder from state
                onFolderClick = { },
                showStarredCount = state.showStarredCount,
            )
        }
    }
}

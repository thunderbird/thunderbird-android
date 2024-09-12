package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.navigation.drawer.ui.FakeData.DISPLAY_ACCOUNT
import kotlinx.collections.immutable.persistentListOf

@Composable
@Preview(showBackground = true)
internal fun DrawerContentPreview() {
    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                currentAccount = null,
                accounts = persistentListOf(),
                folders = persistentListOf(),
            ),
        )
    }
}

@Composable
@Preview(showBackground = true)
fun DrawerContentWithAccountPreview() {
    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = persistentListOf(DISPLAY_ACCOUNT),
                currentAccount = DISPLAY_ACCOUNT,
            ),
        )
    }
}

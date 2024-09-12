package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.navigation.drawer.ui.account.FakeData.DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun DrawerContentPreview() {
    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = emptyList(),
                currentAccount = null,
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
                accounts = listOf(DISPLAY_ACCOUNT),
                currentAccount = DISPLAY_ACCOUNT,
            ),
        )
    }
}

package app.k9mail.feature.account.setup.ui.options

import androidx.compose.runtime.Composable

@Composable
fun AccountOptionsScreen(
    onFinishClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    AccountOptionsContent(
        onFinishClick = onFinishClick,
        onBackClick = onBackClick,
    )
}

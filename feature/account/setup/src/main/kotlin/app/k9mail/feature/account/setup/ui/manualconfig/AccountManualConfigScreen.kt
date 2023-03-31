package app.k9mail.feature.account.setup.ui.manualconfig

import androidx.compose.runtime.Composable

@Composable
fun AccountManualConfigScreen(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    AccountManualConfigContent(
        onNextClick = onNextClick,
        onBackClick = onBackClick,
    )
}

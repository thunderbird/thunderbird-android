package app.k9mail.feature.account.setup.ui.autoconfig

import androidx.compose.runtime.Composable

@Composable
fun AccountAutoConfigScreen(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    AccountAutoConfigContent(
        onNextClick = onNextClick,
        onBackClick = onBackClick,
    )
}

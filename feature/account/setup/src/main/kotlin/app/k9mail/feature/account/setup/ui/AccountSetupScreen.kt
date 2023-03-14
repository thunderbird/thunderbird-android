package app.k9mail.feature.account.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AccountSetupScreen(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    AccountSetupContent(
        onNextClick = onNextClick,
        onBackClick = onBackClick,
    )
}

@Preview(showBackground = true)
@Composable
internal fun AccountSetupScreenPreview() {
    AccountSetupContent({}, {})
}

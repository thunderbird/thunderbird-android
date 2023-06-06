package app.k9mail.feature.account.setup.ui.autoconfig

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.common.AccountSetupBottomBar
import app.k9mail.feature.account.setup.ui.common.AccountSetupTopHeader

@Composable
fun AccountAutoConfigScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AccountSetupTopHeader()
        },
        bottomBar = {
            AccountSetupBottomBar(
                nextButtonText = stringResource(id = R.string.account_setup_button_next),
                backButtonText = stringResource(id = R.string.account_setup_button_back),
                onNextClick = onNext,
                onBackClick = onBack,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        AccountAutoConfigContent(
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoConfigScreenK9Preview() {
    K9Theme {
        AccountAutoConfigScreen(
            onNext = {},
            onBack = {},
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoConfigScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountAutoConfigScreen(
            onNext = {},
            onBack = {},
        )
    }
}

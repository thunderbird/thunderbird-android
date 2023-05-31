package app.k9mail.feature.account.setup.ui.incoming

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.common.AccountSetupBottomBar
import app.k9mail.feature.account.setup.ui.common.AccountSetupTopAppBar

@Composable
fun AccountIncomingConfigScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AccountSetupTopAppBar(
                title = stringResource(id = R.string.account_setup_incoming_config_top_bar_title),
            )
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
        AccountIncomingConfigContent(
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigScreenK9Preview() {
    K9Theme {
        AccountIncomingConfigScreen(
            onNext = {},
            onBack = {},
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountIncomingConfigScreen(
            onNext = {},
            onBack = {},
        )
    }
}

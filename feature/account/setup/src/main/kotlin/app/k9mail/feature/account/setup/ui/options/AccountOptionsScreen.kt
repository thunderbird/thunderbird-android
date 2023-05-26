package app.k9mail.feature.account.setup.ui.options

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R.string
import app.k9mail.feature.account.setup.ui.common.AccountSetupBottomBar
import app.k9mail.feature.account.setup.ui.common.AccountSetupTopAppBar

@Composable
internal fun AccountOptionsScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AccountSetupTopAppBar(
                title = stringResource(id = string.account_setup_options_top_bar_title),
            )
        },
        bottomBar = {
            AccountSetupBottomBar(
                nextButtonText = stringResource(id = string.account_setup_button_finish),
                backButtonText = stringResource(id = string.account_setup_button_back),
                onNextClick = onNext,
                onBackClick = onBack,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        AccountOptionsContent(
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOptionsScreenK9Preview() {
    K9Theme {
        AccountOptionsScreen(
            onNext = {},
            onBack = {},
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOptionsScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountOptionsScreen(
            onNext = {},
            onBack = {},
        )
    }
}

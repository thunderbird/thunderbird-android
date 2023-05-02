package app.k9mail.feature.account.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigScreen
import app.k9mail.feature.account.setup.ui.manualconfig.AccountManualConfigScreen
import app.k9mail.feature.account.setup.ui.options.AccountOptionsScreen

@Composable
fun AccountSetupScreen(
    onFinishClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val accountSetupSteps = remember { mutableStateOf(AccountSetupSteps.AUTO_CONFIG) }

    when (accountSetupSteps.value) {
        AccountSetupSteps.AUTO_CONFIG -> {
            AccountAutoConfigScreen(
                onNextClick = {
                    // TODO validate config
                    accountSetupSteps.value = AccountSetupSteps.MANUAL_CONFIG
                },
                onBackClick = onBackClick,
            )
        }

        AccountSetupSteps.MANUAL_CONFIG -> {
            AccountManualConfigScreen(
                onNextClick = {
                    accountSetupSteps.value = AccountSetupSteps.OPTIONS
                },
                onBackClick = {
                    accountSetupSteps.value = AccountSetupSteps.AUTO_CONFIG
                },
            )
        }

        AccountSetupSteps.OPTIONS -> {
            AccountOptionsScreen(
                // validate account
                onFinishClick = onFinishClick,
                onBackClick = {
                    accountSetupSteps.value = AccountSetupSteps.MANUAL_CONFIG
                },
            )
        }
    }
}

enum class AccountSetupSteps {
    AUTO_CONFIG,
    MANUAL_CONFIG,
    OPTIONS,
}

@Preview(showBackground = true)
@Composable
internal fun AccountSetupScreenPreview() {
    AccountSetupScreen(
        onFinishClick = {},
        onBackClick = {},
    )
}

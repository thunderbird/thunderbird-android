package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.common.ui.PreviewWithThemeAndKoin
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.fake.fakeAutoDiscoveryResultSettings

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentPreview() {
    PreviewWithThemeAndKoin {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentEmailPreview() {
    PreviewWithThemeAndKoin {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                emailAddress = StringInputField(value = "test@example.com"),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentPasswordPreview() {
    PreviewWithThemeAndKoin {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "test@example.com"),
                autoDiscoverySettings = fakeAutoDiscoveryResultSettings(isTrusted = true),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentPasswordUntrustedSettingsPreview() {
    PreviewWithThemeAndKoin {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "test@example.com"),
                autoDiscoverySettings = fakeAutoDiscoveryResultSettings(isTrusted = false),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentPasswordNoSettingsPreview() {
    PreviewWithThemeAndKoin {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "test@example.com"),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentOAuthPreview() {
    PreviewWithThemeAndKoin {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.OAUTH,
                emailAddress = StringInputField(value = "test@example.com"),
                autoDiscoverySettings = fakeAutoDiscoveryResultSettings(isTrusted = true),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
        )
    }
}

package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.fake.fakeAutoDiscoveryResultSettings
import net.thunderbird.core.validation.input.StringInputField

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentPreview() {
    PreviewWithTheme {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
            brandName = "BrandName",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentEmailPreview() {
    PreviewWithTheme {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                emailAddress = StringInputField(value = "test@example.com"),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
            brandName = "BrandName",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentPasswordPreview() {
    PreviewWithTheme {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "test@example.com"),
                autoDiscoverySettings = fakeAutoDiscoveryResultSettings(isTrusted = true),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
            brandName = "BrandName",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentPasswordUntrustedSettingsPreview() {
    PreviewWithTheme {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "test@example.com"),
                autoDiscoverySettings = fakeAutoDiscoveryResultSettings(isTrusted = false),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
            brandName = "BrandName",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentPasswordNoSettingsPreview() {
    PreviewWithTheme {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "test@example.com"),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
            brandName = "BrandName",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAutoDiscoveryContentOAuthPreview() {
    PreviewWithTheme {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.OAUTH,
                emailAddress = StringInputField(value = "test@example.com"),
                autoDiscoverySettings = fakeAutoDiscoveryResultSettings(isTrusted = true),
            ),
            onEvent = {},
            oAuthViewModel = FakeAccountOAuthViewModel(),
            brandName = "BrandName",
        )
    }
}

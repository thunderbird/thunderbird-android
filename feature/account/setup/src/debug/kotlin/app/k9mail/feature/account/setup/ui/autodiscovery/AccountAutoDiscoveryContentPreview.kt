package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.fake.fakeAutoDiscoveryResultSettings
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@PreviewLightDark
@PreviewDevices
internal fun AccountAutoDiscoveryContentPreview() {
    ThundermailPreview {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(),
            onEvent = {},
            oAuthViewModel = viewModel { FakeAccountOAuthViewModel() },
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@PreviewLightDark
internal fun AccountAutoDiscoveryContentEmailPreview() {
    ThundermailPreview {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                emailAddress = StringInputField(value = "test@example.com"),
            ),
            onEvent = {},
            oAuthViewModel = viewModel { FakeAccountOAuthViewModel() },
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@PreviewLightDark
internal fun AccountAutoDiscoveryContentPasswordPreview() {
    ThundermailPreview {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "test@example.com"),
                autoDiscoverySettings = fakeAutoDiscoveryResultSettings(isTrusted = true),
            ),
            onEvent = {},
            oAuthViewModel = viewModel { FakeAccountOAuthViewModel() },
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@PreviewLightDark
internal fun AccountAutoDiscoveryContentPasswordUntrustedSettingsPreview() {
    ThundermailPreview {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "test@example.com"),
                autoDiscoverySettings = fakeAutoDiscoveryResultSettings(isTrusted = false),
            ),
            onEvent = {},
            oAuthViewModel = viewModel { FakeAccountOAuthViewModel() },
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@PreviewLightDark
internal fun AccountAutoDiscoveryContentPasswordNoSettingsPreview() {
    ThundermailPreview {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "test@example.com"),
            ),
            onEvent = {},
            oAuthViewModel = viewModel { FakeAccountOAuthViewModel() },
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@PreviewLightDark
internal fun AccountAutoDiscoveryContentOAuthPreview() {
    ThundermailPreview {
        AccountAutoDiscoveryContent(
            state = AccountAutoDiscoveryContract.State(
                configStep = AccountAutoDiscoveryContract.ConfigStep.OAUTH,
                emailAddress = StringInputField(value = "test@example.com"),
                autoDiscoverySettings = fakeAutoDiscoveryResultSettings(isTrusted = true),
            ),
            onEvent = {},
            oAuthViewModel = viewModel { FakeAccountOAuthViewModel() },
            contentPadding = PaddingValues(),
        )
    }
}

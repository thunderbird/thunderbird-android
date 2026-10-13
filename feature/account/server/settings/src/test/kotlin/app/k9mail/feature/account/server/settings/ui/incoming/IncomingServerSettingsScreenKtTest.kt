package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onAllNodesWithText
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.server.settings.R
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.validation.input.StringInputField
import org.junit.Test

class IncomingServerSettingsScreenKtTest : ComposeTest() {

    @Test
    fun `account creation labels the protocol picker as Connector`() = runComposeTest {
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(protocolType = IncomingProtocolType.IMAP),
                onEvent = {},
            )
        }

        onNodeWithText(getString(R.string.account_server_settings_connector_label)).assertIsDisplayed()
        onAllNodesWithText(getString(R.string.account_server_settings_protocol_type_label)).assertCountEquals(0)
    }

    @Test
    fun `Graph protocol hides the unused username field`() = runComposeTest {
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(
                    protocolType = IncomingProtocolType.GRAPH,
                    username = StringInputField(value = "user@example.com"),
                ),
                onEvent = {},
            )
        }

        onAllNodesWithText(getString(R.string.account_server_settings_username_label)).assertCountEquals(0)
        onAllNodesWithText(getString(R.string.account_server_settings_client_certificate_label)).assertCountEquals(0)
    }

    @Test
    fun `IMAP protocol keeps the username field visible`() = runComposeTest {
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(
                    protocolType = IncomingProtocolType.IMAP,
                    username = StringInputField(value = "user@example.com"),
                ),
                onEvent = {},
            )
        }

        onNodeWithText(getString(R.string.account_server_settings_username_label)).assertIsDisplayed()
    }

    @Test
    fun `Zoho redirect URI field is editable and emits the user's exact value`() = runComposeTest {
        val events = mutableListOf<Event>()
        val redirectUri = "net.thunderbird.android.debug://custom-zoho-callback"

        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(
                    protocolType = IncomingProtocolType.ZOHO_MAIL_API,
                    zohoRedirectUri = StringInputField(value = redirectUri),
                ),
                onEvent = events::add,
            )
        }

        val redirectUriField = onNodeWithText(redirectUri)
        redirectUriField.assertIsEnabled()
        redirectUriField.performTextInputSelection(TextRange(redirectUri.length))
        redirectUriField.performTextInput("/edited")

        assertThat(events.last()).isEqualTo(Event.ZohoRedirectUriChanged("$redirectUri/edited"))
    }

    @Test
    fun `Zoho email remains editable while name and incoming-only option are removed`() = runComposeTest {
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(protocolType = IncomingProtocolType.ZOHO_MAIL_API),
                onEvent = {},
            )
        }

        onAllNodesWithText(getString(R.string.account_server_settings_zoho_name_label)).assertCountEquals(0)
        onNodeWithText(getString(R.string.account_server_settings_zoho_email_label)).assertIsDisplayed()
        onAllNodesWithText(getString(R.string.account_server_settings_zoho_incoming_only_label)).assertCountEquals(0)
    }

    @Test
    fun `Zoho identity fields are not editable from Incoming Server settings`() = runComposeTest {
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Edit,
                state = State(protocolType = IncomingProtocolType.ZOHO_MAIL_API),
                onEvent = {},
            )
        }

        onAllNodesWithText(getString(R.string.account_server_settings_zoho_name_label)).assertCountEquals(0)
        onAllNodesWithText(getString(R.string.account_server_settings_zoho_email_label)).assertCountEquals(0)
    }

    @Test
    fun `empty Zoho redirect URI defaults to the double-slash app callback`() = runComposeTest {
        val events = mutableListOf<Event>()
        var packageName = ""

        setContentWithTheme {
            packageName = LocalContext.current.packageName
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(protocolType = IncomingProtocolType.ZOHO_MAIL_API),
                onEvent = events::add,
            )
        }

        onNodeWithText(getString(R.string.account_server_settings_zoho_redirect_uri_label)).assertIsDisplayed()

        assertThat(events).isEqualTo(
            listOf(Event.ZohoRedirectUriChanged("$packageName://oauth2redirect")),
        )
    }

    @Test
    fun `Graph reauthorization action is no longer shown in Incoming Server settings`() = runComposeTest {
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Edit,
                state = State(protocolType = IncomingProtocolType.GRAPH),
                onEvent = {},
            )
        }

        onAllNodesWithText(getString(R.string.account_server_settings_reauthorize_microsoft_account))
            .assertCountEquals(0)
    }

    @Test
    fun `Zoho reauthorization action is no longer shown in Incoming Server settings`() = runComposeTest {
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Edit,
                state = State(protocolType = IncomingProtocolType.ZOHO_MAIL_API),
                onEvent = {},
            )
        }

        onAllNodesWithText(getString(R.string.account_server_settings_reauthorize_zoho_account))
            .assertCountEquals(0)
    }

    @Test
    fun `Gmail API creation exposes an editable Redirect URI`() = runComposeTest {
        val events = mutableListOf<Event>()
        val redirectUri = "net.thunderbird.android.debug:/oauth2redirect"
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(
                    protocolType = IncomingProtocolType.GMAIL_API,
                    apiOAuthRedirectUri = StringInputField(value = redirectUri),
                ),
                onEvent = events::add,
            )
        }

        onNodeWithText(getString(R.string.account_server_settings_api_client_id_label)).assertIsDisplayed()
        onNodeWithText(getString(R.string.account_server_settings_api_redirect_uri_label)).assertIsDisplayed()
        val redirectUriInput = onNodeWithText(redirectUri)
        redirectUriInput.assertIsEnabled()
        redirectUriInput.performTextInputSelection(TextRange(redirectUri.length))
        redirectUriInput.performTextInput("/custom")

        assertThat(events.last()).isEqualTo(Event.ApiOAuthRedirectUriChanged("$redirectUri/custom"))
    }

    @Test
    fun `Graph creation shows Client ID and Redirect URI fields`() = runComposeTest {
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(protocolType = IncomingProtocolType.GRAPH),
                onEvent = {},
            )
        }

        onNodeWithText(getString(R.string.account_server_settings_api_client_id_label)).assertIsDisplayed()
        onNodeWithText(getString(R.string.account_server_settings_api_redirect_uri_label)).assertIsDisplayed()
    }

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeIncomingServerSettingsViewModel(initialState = initialState)
        var onNextCounter = 0
        var onBackCounter = 0

        setContentWithTheme {
            IncomingServerSettingsScreen(
                onNext = { onNextCounter++ },
                onBack = { onBackCounter++ },
                viewModel = viewModel,
            )
        }

        assertThat(onNextCounter).isEqualTo(0)
        assertThat(onBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateNext)

        assertThat(onNextCounter).isEqualTo(1)
        assertThat(onBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateBack)

        assertThat(onNextCounter).isEqualTo(1)
        assertThat(onBackCounter).isEqualTo(1)
    }
}

package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.server.settings.R
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import org.junit.Test

class IncomingServerSettingsContentKtTest : ComposeTest() {

    @Test
    fun `should show IMAP hint when creating a POP3 account`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(protocolType = IncomingProtocolType.POP3),
                onEvent = {},
            )
        }

        // Assert
        onNodeWithText(getString(R.string.account_server_settings_pop3_hint_title)).assertIsDisplayed()
        onNodeWithText(getString(R.string.account_server_settings_pop3_hint_text)).assertIsDisplayed()
    }

    @Test
    fun `should not show IMAP hint when creating an IMAP account`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Create,
                state = State(protocolType = IncomingProtocolType.IMAP),
                onEvent = {},
            )
        }

        // Assert
        onNodeWithText(getString(R.string.account_server_settings_pop3_hint_title)).assertDoesNotExist()
        onNodeWithText(getString(R.string.account_server_settings_pop3_hint_text)).assertDoesNotExist()
    }

    @Test
    fun `should not show IMAP hint when editing a POP3 account`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            IncomingServerSettingsContent(
                mode = InteractionMode.Edit,
                state = State(protocolType = IncomingProtocolType.POP3),
                onEvent = {},
            )
        }

        // Assert
        onNodeWithText(getString(R.string.account_server_settings_pop3_hint_title)).assertDoesNotExist()
        onNodeWithText(getString(R.string.account_server_settings_pop3_hint_text)).assertDoesNotExist()
    }
}

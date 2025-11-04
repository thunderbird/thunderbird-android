
package app.k9mail.feature.account.server.settings.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.account.server.settings.R
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.server.settings.ui.common.AuthenticationError
import net.thunderbird.feature.account.server.settings.ui.common.Authenticator
import org.junit.Test
import app.k9mail.core.ui.compose.designsystem.R as RDesign

class ProtectedPasswordInputKtTest : ComposeTest() {

    @Test
    fun `should display default label from wrapper`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            ProtectedPasswordInput(
                password = "",
                onPasswordChange = {},
                authenticator = Authenticator { Outcome.Failure(AuthenticationError.Failed) },
            )
        }

        // Assert
        onNodeWithText(getString(RDesign.string.designsystem_molecule_password_input_label))
            .assertIsDisplayed()
    }

    @Test
    fun `should show warning message when authenticator fails`() = runComposeTest {
        // Arrange
        val password = "Password input"
        val failingAuthenticator: Authenticator = Authenticator { Outcome.Failure(AuthenticationError.NotAvailable) }
        setContentWithTheme {
            ProtectedPasswordInput(
                password = password,
                onPasswordChange = {},
                authenticator = failingAuthenticator,
            )
        }

        // Act
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_show_password),
        ).performClick()

        // Assert
        onNodeWithText(
            getString(R.string.account_server_settings_password_authentication_screen_lock_required),
        ).assertIsDisplayed()
    }

    @Test
    fun `should show error message when provided`() = runComposeTest {
        // Arrange
        val errorMessage = "Some error"
        setContentWithTheme {
            ProtectedPasswordInput(
                password = "",
                onPasswordChange = {},
                errorMessage = errorMessage,
                authenticator = Authenticator { Outcome.Failure(AuthenticationError.Failed) },
            )
        }

        // Assert
        onNodeWithText(errorMessage).assertIsDisplayed()
    }
}

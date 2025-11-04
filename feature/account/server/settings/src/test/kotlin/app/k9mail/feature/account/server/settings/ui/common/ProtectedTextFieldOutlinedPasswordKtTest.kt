package app.k9mail.feature.account.server.settings.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.server.settings.ui.common.AuthenticationError
import org.junit.Test
import app.k9mail.core.ui.compose.designsystem.R as RDesign

class ProtectedTextFieldOutlinedPasswordKtTest : ComposeTest() {

    @Test
    fun `should not reveal password when not authorized`() = runComposeTest {
        // Arrange
        var value = VALUE
        setContentWithTheme {
            ProtectedTextFieldOutlinedPassword(
                value = value,
                onValueChange = { value = it },
                onWarningChange = { _ -> },
                authenticator = { Outcome.Failure(AuthenticationError.Failed) },
            )
        }

        // Act
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_show_password),
        ).performClick()

        // Assert
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_hide_password),
        ).assertIsNotDisplayed()
        onNodeWithText(VALUE_MASKED).assertIsDisplayed()
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_show_password),
        ).assertIsDisplayed()
    }

    @Test
    fun `should not reveal value when not authorized and value is empty`() = runComposeTest {
        // Arrange
        var value = ""
        setContent {
            ProtectedTextFieldOutlinedPassword(
                value = value,
                onValueChange = { value = it },
                onWarningChange = { _ -> },
                authenticator = { Outcome.Failure(AuthenticationError.Failed) },
            )
        }

        // Act
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_show_password),
        ).performClick()

        // Assert
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_hide_password),
        ).assertIsNotDisplayed()
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_show_password),
        ).assertIsDisplayed()
    }

    @Test
    fun `should reveal value when authorized`() = runComposeTest {
        // Arrange
        val value = VALUE
        setContent {
            ProtectedTextFieldOutlinedPassword(
                value = value,
                onValueChange = {},
                onWarningChange = {},
                authenticator = { Outcome.Success(Unit) },
            )
        }

        // Act
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_show_password),
        ).performClick()

        // Assert
        onNodeWithText(VALUE_MASKED).assertIsNotDisplayed()
        onNodeWithText(VALUE).assertIsDisplayed()
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_hide_password),
        ).assertIsDisplayed()
    }

    @Test
    fun `should reveal empty value when authorized`() = runComposeTest {
        // Arrange
        val value = ""
        setContent {
            ProtectedTextFieldOutlinedPassword(
                value = value,
                onValueChange = {},
                onWarningChange = {},
                authenticator = { Outcome.Success(Unit) },
            )
        }

        // Act
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_show_password),
        ).performClick()

        // Assert
        onNodeWithContentDescription(
            getString(RDesign.string.designsystem_atom_password_textfield_hide_password),
        ).assertIsDisplayed()
    }

    private companion object Companion {
        const val VALUE = "Password input"

        // 14 dots to match length of "Password input"
        const val VALUE_MASKED = "••••••••••••••"
    }
}

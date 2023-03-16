package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.testing.ComposeTest
import org.junit.Test

private const val PASSWORD = "Password input"

class PasswordTextFieldOutlinedKtTest : ComposeTest() {

    @Test
    fun `should not display password by default`() = runComposeTest {
        setContent {
            PasswordTextFieldOutlined(
                value = PASSWORD,
                onValueChange = {},
            )
        }

        onNodeWithText(PASSWORD).assertDoesNotExist()
    }

    @Test
    fun `should display password when show password is clicked`() = runComposeTest {
        setContent {
            PasswordTextFieldOutlined(
                value = PASSWORD,
                onValueChange = {},
            )
        }

        onShowPasswordNode().performClick()

        onNodeWithText(PASSWORD).assertIsDisplayed()
    }

    @Test
    fun `should not display password when hide password is clicked`() = runComposeTest {
        setContent {
            PasswordTextFieldOutlined(
                value = PASSWORD,
                onValueChange = {},
            )
        }
        onShowPasswordNode().performClick()

        onHidePasswordNode().performClick()

        onNodeWithText(PASSWORD).assertDoesNotExist()
    }

    @Test
    fun `should display hide password icon when show password is clicked`() = runComposeTest {
        setContent {
            PasswordTextFieldOutlined(
                value = PASSWORD,
                onValueChange = {},
            )
        }

        onShowPasswordNode().performClick()

        onHidePasswordNode().assertIsDisplayed()
    }

    @Test
    fun `should display show password icon when hide password icon is clicked`() = runComposeTest {
        setContent {
            PasswordTextFieldOutlined(
                value = PASSWORD,
                onValueChange = {},
            )
        }
        onShowPasswordNode().performClick()

        onHidePasswordNode().performClick()

        onShowPasswordNode().assertIsDisplayed()
    }

    private fun SemanticsNodeInteractionsProvider.onShowPasswordNode(): SemanticsNodeInteraction {
        return onNodeWithContentDescription(
            getString(R.string.designsystem_atom_password_textfield_show_password),
        )
    }

    private fun SemanticsNodeInteractionsProvider.onHidePasswordNode(): SemanticsNodeInteraction {
        return onNodeWithContentDescription(
            getString(R.string.designsystem_atom_password_textfield_hide_password),
        )
    }
}

package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithText
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import org.junit.Test

private const val PASSWORD = "Password input"
private const val PASSWORD_MASKED = "••••••••••••••"
private const val TEST_TAG = "TextFieldOutlinedPassword"

class TextFieldOutlinedPasswordKtTest : ComposeTest() {

    @Test
    fun `should not display password by default`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
            )
        }

        onNodeWithText(PASSWORD_MASKED).assertIsDisplayed()
    }

    @Test
    fun `should display password when show password is clicked`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
            )
        }

        onShowPasswordNode().performClick()

        onNodeWithText(PASSWORD_MASKED).assertIsNotDisplayed()
        onNodeWithText(PASSWORD).assertIsDisplayed()
    }

    @Test
    fun `should not display password when hide password is clicked`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
            )
        }
        onShowPasswordNode().performClick()

        onHidePasswordNode().performClick()

        onNodeWithText(PASSWORD_MASKED).assertIsDisplayed()
    }

    @Test
    fun `should display hide password icon when show password is clicked`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
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
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
            )
        }
        onShowPasswordNode().performClick()

        onHidePasswordNode().performClick()

        onShowPasswordNode().assertIsDisplayed()
    }

    @Test
    fun `should call callback when password visibility toggle icon is clicked`() = runComposeTest {
        var clicked = false
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
                isPasswordVisible = false,
                onPasswordVisibilityToggleClicked = { clicked = true },
            )
        }

        onShowPasswordNode().performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `should display password when isPasswordVisible = true`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
                isPasswordVisible = true,
                onPasswordVisibilityToggleClicked = {},
            )
        }

        onNodeWithText(PASSWORD_MASKED).assertIsNotDisplayed()
        onNodeWithText(PASSWORD).assertIsDisplayed()
    }

    @Test
    fun `should not display password when isPasswordVisible = false`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
                isPasswordVisible = false,
                onPasswordVisibilityToggleClicked = {},
            )
        }

        onNodeWithText(PASSWORD_MASKED).assertIsDisplayed()
    }

    @Test
    fun `variant 1 should call onValueChange when value changes`() = runComposeTest {
        var value = "initial"
        setContent {
            TextFieldOutlinedPassword(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTagAsResourceId(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput(" + added text")

        assertThat(value).isEqualTo("initial + added text")
    }

    @Test
    fun `variant 2 should call onValueChange when value changes`() = runComposeTest {
        var value = "initial"
        setContent {
            TextFieldOutlinedPassword(
                value = value,
                onValueChange = { value = it },
                isPasswordVisible = false,
                onPasswordVisibilityToggleClicked = {},
                modifier = Modifier.testTagAsResourceId(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput(" + added text")

        assertThat(value).isEqualTo("initial + added text")
    }

    @Test
    fun `variant 1 should strip line breaks before onValueChange is called`() = runComposeTest {
        var value = ""
        setContent {
            TextFieldOutlinedPassword(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTagAsResourceId(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput("one\n two")

        assertThat(value).isEqualTo("one two")
    }

    @Test
    fun `variant 2 should strip line breaks before onValueChange is called`() = runComposeTest {
        var value = ""
        setContent {
            TextFieldOutlinedPassword(
                value = value,
                onValueChange = { value = it },
                isPasswordVisible = false,
                onPasswordVisibilityToggleClicked = {},
                modifier = Modifier.testTagAsResourceId(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput("one\n two")

        assertThat(value).isEqualTo("one two")
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

package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import net.thunderbird.components.ui.testing.ComposeUiTestHarness
import net.thunderbird.components.ui.testing.ComposeUiTestScope
import org.junit.Test

private const val PASSWORD = "Password input"
private const val PASSWORD_MASKED = "••••••••••••••"
private const val SHOW_PASSWORD_DESCRIPTION = "Show password"
private const val HIDE_PASSWORD_DESCRIPTION = "Hide password"
private const val TEST_TAG = "TextFieldOutlinedPassword"

class TextFieldOutlinedPasswordKtTest : ComposeUiTestHarness() {

    @Test
    fun `should not display password by default`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onPasswordTextField().assert(hasEditableText(PASSWORD_MASKED))
    }

    @Test
    fun `should display password when show password is clicked`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onShowPasswordNode().performClick()

        onPasswordTextField().assert(hasEditableText(PASSWORD))
    }

    @Test
    fun `should not display password when hide password is clicked`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        onShowPasswordNode().performClick()

        onHidePasswordNode().performClick()

        onPasswordTextField().assert(hasEditableText(PASSWORD_MASKED))
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
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onPasswordTextField().assert(hasEditableText(PASSWORD))
    }

    @Test
    fun `should not display password when isPasswordVisible = false`() = runComposeTest {
        setContent {
            TextFieldOutlinedPassword(
                value = PASSWORD,
                onValueChange = {},
                isPasswordVisible = false,
                onPasswordVisibilityToggleClicked = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onPasswordTextField().assert(hasEditableText(PASSWORD_MASKED))
    }

    @Test
    fun `variant 1 should call onValueChange when value changes`() = runComposeTest {
        var value = "initial"
        setContent {
            TextFieldOutlinedPassword(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(TEST_TAG),
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
                modifier = Modifier.testTag(TEST_TAG),
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
                modifier = Modifier.testTag(TEST_TAG),
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
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput("one\n two")

        assertThat(value).isEqualTo("one two")
    }

    private fun ComposeUiTestScope.onShowPasswordNode(): SemanticsNodeInteraction {
        return onNodeWithContentDescription(SHOW_PASSWORD_DESCRIPTION)
    }

    private fun ComposeUiTestScope.onHidePasswordNode(): SemanticsNodeInteraction {
        return onNodeWithContentDescription(HIDE_PASSWORD_DESCRIPTION)
    }

    private fun ComposeUiTestScope.onPasswordTextField(): SemanticsNodeInteraction {
        return onNodeWithTag(TEST_TAG)
    }

    private fun hasEditableText(text: String): SemanticsMatcher {
        return SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text))
    }
}

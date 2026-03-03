package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

private const val TEST_TAG = "TextFieldOutlinedEmailAddress"

class TextFieldOutlinedEmailAddressKtTest : ComposeTest() {

    @Test
    fun `should call onValueChange when value changes`() = runComposeTest {
        var value = "initial"
        setContent {
            TextFieldOutlinedEmailAddress(
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
    fun `should strip line breaks before onValueChange is called`() = runComposeTest {
        var value = ""
        setContent {
            TextFieldOutlinedEmailAddress(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput("one\n two")

        assertThat(value).isEqualTo("one two")
    }
}

package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

private const val TEST_TAG = "TextFieldOutlinedNumber"

class TextFieldOutlinedNumberKtTest : ComposeTest() {

    @Test
    fun `should call onValueChange with null when input is empty`() = runComposeTest {
        var value: Long? = 1L
        setContent {
            TextFieldOutlinedNumber(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextClearance()

        assertThat(value).isEqualTo(null)
    }

    @Test
    fun `should call onValueChange when value changes`() = runComposeTest {
        var value: Long? = 123L
        setContent {
            TextFieldOutlinedNumber(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput("456")

        assertThat(value).isEqualTo(123456L)
    }

    @Test
    fun `should return null when no number`() = runComposeTest {
        var value: Long? = 123L
        setContent {
            TextFieldOutlinedNumber(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput(",")

        assertThat(value).isEqualTo(null)
    }

    @Test
    fun `should return null when input exceeds max long`() = runComposeTest {
        var value: Long? = null
        setContent {
            TextFieldOutlinedNumber(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput("9223372036854775808")

        assertThat(value).isEqualTo(null)
    }
}

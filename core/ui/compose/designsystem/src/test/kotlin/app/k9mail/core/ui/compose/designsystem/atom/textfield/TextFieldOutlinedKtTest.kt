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

private const val TEST_TAG = "TextFieldOutlined"

class TextFieldOutlinedKtTest : ComposeTest() {

    @Test
    fun `should call onValueChange when value changes with isSingleLine = false`() = runComposeTest {
        var value = "initial"
        setContent {
            TextFieldOutlined(
                value = value,
                onValueChange = { value = it },
                isSingleLine = false,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput(" + added text")

        assertThat(value).isEqualTo("initial + added text")
    }

    @Test
    fun `should call onValueChange when value changes with isSingleLine = true`() = runComposeTest {
        var value = "initial"
        setContent {
            TextFieldOutlined(
                value = value,
                onValueChange = { value = it },
                isSingleLine = true,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput(" + added text")

        assertThat(value).isEqualTo("initial + added text")
    }

    @Test
    fun `should allow line breaks when isSingleLine = false`() = runComposeTest {
        var value = ""
        setContent {
            TextFieldOutlined(
                value = value,
                onValueChange = { value = it },
                isSingleLine = false,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput("one\ntwo")

        assertThat(value).isEqualTo("one\ntwo")
    }

    @Test
    fun `should strip line breaks before onValueChange is called when isSingleLine = true`() = runComposeTest {
        var value = ""
        setContent {
            TextFieldOutlined(
                value = value,
                onValueChange = { value = it },
                isSingleLine = true,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()
        onNodeWithTag(TEST_TAG).performTextInput("one\n two")

        assertThat(value).isEqualTo("one two")
    }
}

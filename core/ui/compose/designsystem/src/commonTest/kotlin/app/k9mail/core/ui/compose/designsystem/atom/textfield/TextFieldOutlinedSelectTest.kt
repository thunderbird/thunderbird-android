package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.testing.ComposeUiTestHarness

private const val TEST_TAG = "TextFieldOutlinedSelect"

class TextFieldOutlinedSelectTest : ComposeUiTestHarness() {
    @Test
    fun `should call onValueChange when value changes`() = runComposeTest {
        var value = "option1"
        setContent {
            TextFieldOutlinedSelect(
                options = persistentListOf("option1", "option2"),
                selectedOption = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        onNodeWithTag(TEST_TAG).performClick()

        onNodeWithText("option2").performClick()
        assertThat(value).isEqualTo("option2")
    }

    @Test
    fun `should not show dropdown when not enabled`() = runComposeTest {
        setContent {
            TextFieldOutlinedSelect(
                options = persistentListOf("option1", "option2"),
                selectedOption = "option1",
                onValueChange = {},
                modifier = Modifier.testTag(TEST_TAG),
                isEnabled = false,
            )
        }

        onNodeWithTag(TEST_TAG).performClick()

        onNodeWithText("option2").assertDoesNotExist()
    }

    @Test
    fun `should not show dropdown when read-only`() = runComposeTest {
        setContent {
            TextFieldOutlinedSelect(
                options = persistentListOf("option1", "option2"),
                selectedOption = "option1",
                onValueChange = {},
                modifier = Modifier.testTag(TEST_TAG),
                isReadOnly = true,
            )
        }

        onNodeWithTag(TEST_TAG).performClick()

        onNodeWithText("option2").assertDoesNotExist()
    }
}

package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId

private const val TEST_TAG = "TextFieldOutlinedSelect"

class TextFieldOutlinedSelectTest : ComposeTest() {
    @Test
    fun `should call onValueChange when value changes`() = runComposeTest {
        var value = "option1"
        setContent {
            TextFieldOutlinedSelect(
                options = persistentListOf("option1", "option2"),
                selectedOption = value,
                onValueChange = { value = it },
                modifier = Modifier.testTagAsResourceId(TEST_TAG),
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
                modifier = Modifier.testTagAsResourceId(TEST_TAG),
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
                modifier = Modifier.testTagAsResourceId(TEST_TAG),
                isReadOnly = true,
            )
        }

        onNodeWithTag(TEST_TAG).performClick()

        onNodeWithText("option2").assertDoesNotExist()
    }
}

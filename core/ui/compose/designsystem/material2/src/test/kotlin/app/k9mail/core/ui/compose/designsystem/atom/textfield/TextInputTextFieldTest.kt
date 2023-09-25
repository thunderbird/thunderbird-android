package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

data class TextInputTextFieldTestData(
    val name: String,
    val input: String,
    val content: @Composable (
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier,
    ) -> Unit,
)

@RunWith(ParameterizedRobolectricTestRunner::class)
class TextInputTextFieldTest(
    data: TextInputTextFieldTestData,
) : ComposeTest() {

    private val testSubjectName = data.name
    private val testSubject = data.content
    private val testInput = data.input

    @Test
    fun `should call onValueChange when value changes`() = runComposeTest {
        var value = testInput
        setContent {
            testSubject(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(testSubjectName),
            )
        }

        onNodeWithTag(testSubjectName).performClick()
        onNodeWithTag(testSubjectName).performTextInput(" + added text")

        assertThat(value).isEqualTo("$testInput + added text")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): List<TextInputTextFieldTestData> = listOf(
            TextInputTextFieldTestData(
                name = "TextFieldOutlined",
                input = "value",
                content = { value, onValueChange: (String) -> Unit, modifier ->
                    TextFieldOutlined(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = modifier,
                    )
                },
            ),
            TextInputTextFieldTestData(
                name = "TextFieldOutlinedPassword",
                input = "value",
                content = { value, onValueChange: (String) -> Unit, modifier ->
                    TextFieldOutlinedPassword(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = modifier,
                    )
                },
            ),
            TextInputTextFieldTestData(
                name = "TextFieldOutlinedEmail",
                input = "value",
                content = { value, onValueChange: (String) -> Unit, modifier ->
                    TextFieldOutlinedEmailAddress(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = modifier,
                    )
                },
            ),
        )
    }
}

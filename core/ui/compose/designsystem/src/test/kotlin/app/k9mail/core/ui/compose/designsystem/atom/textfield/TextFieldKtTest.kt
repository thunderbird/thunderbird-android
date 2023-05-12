package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

private const val VALUE = "Input text"
private const val LABEL = "Label"

data class TextFieldTestData(
    val name: String,
    val content: @Composable (
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier,
        enabled: Boolean?,
        label: String?,
        isRequired: Boolean,
    ) -> Unit,
)

@RunWith(ParameterizedRobolectricTestRunner::class)
class TextFieldKtTest(
    data: TextFieldTestData,
) : ComposeTest() {

    private val testSubjectName = data.name
    private val testSubject = data.content

    @Test
    fun `should call onValueChange when value changes`() = runComposeTest {
        var value = VALUE
        setContent {
            testSubject(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(testSubjectName),
                enabled = null,
                label = null,
                isRequired = false,
            )
        }

        onNodeWithTag(testSubjectName).performClick()
        onNodeWithTag(testSubjectName).performTextInput(" + added text")

        assertThat(value).isEqualTo("$VALUE + added text")
    }

    @Test
    fun `should be enabled by default`() = runComposeTest {
        setContent {
            testSubject(
                value = VALUE,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                enabled = null,
                label = null,
                isRequired = false,
            )
        }

        onNodeWithTag(testSubjectName).assertIsEnabled()
    }

    @Test
    fun `should be disabled when enabled is false`() = runComposeTest {
        setContent {
            testSubject(
                value = VALUE,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                enabled = false,
                label = null,
                isRequired = false,
            )
        }

        onNodeWithTag(testSubjectName).assertIsNotEnabled()
    }

    @Test
    fun `should show label when label is not null`() = runComposeTest {
        setContent {
            testSubject(
                value = VALUE,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                enabled = null,
                label = LABEL,
                isRequired = false,
            )
        }

        onNodeWithText(LABEL).assertIsDisplayed()
    }

    @Test
    fun `should show asterisk when isRequired is true`() = runComposeTest {
        setContent {
            testSubject(
                value = VALUE,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                enabled = null,
                label = LABEL,
                isRequired = true,
            )
        }

        onNodeWithText("$LABEL*").assertIsDisplayed()
    }

    @Test
    fun `should not show asterisk when isRequired is false`() = runComposeTest {
        setContent {
            testSubject(
                value = VALUE,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                enabled = null,
                label = LABEL,
                isRequired = false,
            )
        }

        onNodeWithText("$LABEL*").assertDoesNotExist()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        @Suppress("LongMethod")
        fun data(): List<TextFieldTestData> = listOf(
            TextFieldTestData(
                name = "TextFieldOutlined",
                content = { value, onValueChange, modifier, enabled, label, isRequired ->
                    if (enabled != null) {
                        TextFieldOutlined(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            enabled = enabled,
                            label = label,
                            isRequired = isRequired,
                        )
                    } else {
                        TextFieldOutlined(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            label = label,
                            isRequired = isRequired,
                        )
                    }
                },
            ),
            TextFieldTestData(
                name = "TextFieldOutlinedPassword",
                content = { value, onValueChange, modifier, enabled, label, isRequired ->
                    if (enabled != null) {
                        TextFieldOutlinedPassword(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            enabled = enabled,
                            label = label,
                            isRequired = isRequired,
                        )
                    } else {
                        TextFieldOutlinedPassword(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            label = label,
                            isRequired = isRequired,
                        )
                    }
                },
            ),
            TextFieldTestData(
                name = "TextFieldOutlinedEmail",
                content = { value, onValueChange, modifier, enabled, label, isRequired ->
                    if (enabled != null) {
                        TextFieldOutlinedEmailAddress(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            enabled = enabled,
                            label = label,
                            isRequired = isRequired,
                        )
                    } else {
                        TextFieldOutlinedEmailAddress(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            label = label,
                            isRequired = isRequired,
                        )
                    }
                },
            ),
        )
    }
}

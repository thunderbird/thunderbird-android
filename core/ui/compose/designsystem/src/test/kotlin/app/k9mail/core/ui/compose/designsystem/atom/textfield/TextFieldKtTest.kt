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
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

private const val LABEL = "Label"

data class TextFieldConfig(
    val label: String?,
    val isEnabled: Boolean?,
    val isReadOnly: Boolean,
    val isRequired: Boolean,
)

data class TextFieldTestData<INPUT, VALUE>(
    val name: String,
    val input: INPUT,
    val content: @Composable (
        value: VALUE,
        onValueChange: (VALUE) -> Unit,
        modifier: Modifier,
        textFieldConfig: TextFieldConfig,
    ) -> Unit,
)

@RunWith(ParameterizedRobolectricTestRunner::class)
class TextFieldKtTest(
    data: TextFieldTestData<Any, Any>,
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
                textFieldConfig = TextFieldConfig(
                    label = null,
                    isEnabled = null,
                    isReadOnly = false,
                    isRequired = false,
                ),
            )
        }

        onNodeWithTag(testSubjectName).performClick()
        onNodeWithTag(testSubjectName).performTextInput(" + added text")

        assertThat(value).isEqualTo("$testInput + added text")
    }

    @Test
    fun `should be enabled by default`() = runComposeTest {
        setContent {
            testSubject(
                value = testInput,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                textFieldConfig = TextFieldConfig(
                    label = null,
                    isEnabled = null,
                    isReadOnly = false,
                    isRequired = false,
                ),
            )
        }

        onNodeWithTag(testSubjectName).assertIsEnabled()
    }

    @Test
    fun `should be disabled when enabled is false`() = runComposeTest {
        setContent {
            testSubject(
                value = testInput,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                textFieldConfig = TextFieldConfig(
                    label = null,
                    isEnabled = false,
                    isReadOnly = false,
                    isRequired = false,
                ),
            )
        }

        onNodeWithTag(testSubjectName).assertIsNotEnabled()
    }

    @Test
    fun `should show label when label is not null`() = runComposeTest {
        setContent {
            testSubject(
                value = testInput,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                textFieldConfig = TextFieldConfig(
                    label = LABEL,
                    isEnabled = null,
                    isReadOnly = false,
                    isRequired = false,
                ),
            )
        }

        onNodeWithText(LABEL).assertIsDisplayed()
    }

    @Test
    fun `should show asterisk when isRequired is true`() = runComposeTest {
        setContent {
            testSubject(
                value = testInput,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                textFieldConfig = TextFieldConfig(
                    label = LABEL,
                    isEnabled = null,
                    isReadOnly = false,
                    isRequired = true,
                ),
            )
        }

        onNodeWithText("$LABEL*").assertIsDisplayed()
    }

    @Test
    fun `should not show asterisk when isRequired is false`() = runComposeTest {
        setContent {
            testSubject(
                value = testInput,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                textFieldConfig = TextFieldConfig(
                    label = LABEL,
                    isEnabled = null,
                    isReadOnly = false,
                    isRequired = false,
                ),
            )
        }

        onNodeWithText("$LABEL*").assertDoesNotExist()
    }

    @Test
    fun `should not allow editing when isReadOnly is true`() = runComposeTest {
        setContent {
            testSubject(
                value = testInput,
                onValueChange = {},
                modifier = Modifier.testTag(testSubjectName),
                textFieldConfig = TextFieldConfig(
                    label = LABEL,
                    isEnabled = null,
                    isReadOnly = true,
                    isRequired = false,
                ),
            )
        }

        onNodeWithTag(testSubjectName).performClick()
        assertFailure {
            onNodeWithText(testSubjectName).performTextInput(" + added text")
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        @Suppress("LongMethod")
        fun data(): List<TextFieldTestData<*, *>> = listOf(
            TextFieldTestData(
                name = "TextFieldOutlined",
                input = "value",
                content = { value, onValueChange: (String) -> Unit, modifier, config ->
                    if (config.isEnabled != null) {
                        TextFieldOutlined(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            label = config.label,
                            isEnabled = config.isEnabled,
                            isReadOnly = config.isReadOnly,
                            isRequired = config.isRequired,
                        )
                    } else {
                        TextFieldOutlined(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            label = config.label,
                            isRequired = config.isRequired,
                            isReadOnly = config.isReadOnly,
                        )
                    }
                },
            ),
            TextFieldTestData(
                name = "TextFieldOutlinedPassword",
                input = "value",
                content = { value, onValueChange: (String) -> Unit, modifier, config ->
                    if (config.isEnabled != null) {
                        TextFieldOutlinedPassword(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            label = config.label,
                            isEnabled = config.isEnabled,
                            isReadOnly = config.isReadOnly,
                            isRequired = config.isRequired,
                        )
                    } else {
                        TextFieldOutlinedPassword(
                            value = value,
                            onValueChange = onValueChange,
                            label = config.label,
                            modifier = modifier,
                            isRequired = config.isRequired,
                            isReadOnly = config.isReadOnly,
                        )
                    }
                },
            ),
            TextFieldTestData(
                name = "TextFieldOutlinedEmail",
                input = "value",
                content = { value, onValueChange: (String) -> Unit, modifier, config ->
                    if (config.isEnabled != null) {
                        TextFieldOutlinedEmailAddress(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            label = config.label,
                            isEnabled = config.isEnabled,
                            isReadOnly = config.isReadOnly,
                            isRequired = config.isRequired,
                        )
                    } else {
                        TextFieldOutlinedEmailAddress(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = modifier,
                            label = config.label,
                            isRequired = config.isRequired,
                            isReadOnly = config.isReadOnly,
                        )
                    }
                },
            ),
        )
    }
}

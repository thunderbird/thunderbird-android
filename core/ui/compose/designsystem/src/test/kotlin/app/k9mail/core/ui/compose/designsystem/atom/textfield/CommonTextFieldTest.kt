package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertFailure
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
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

data class CommonTextFieldTestData(
    val name: String,
    val content: @Composable (
        modifier: Modifier,
        textFieldConfig: TextFieldConfig,
    ) -> Unit,
)

@RunWith(ParameterizedRobolectricTestRunner::class)
class CommonTextFieldTest(
    data: CommonTextFieldTestData,
) : ComposeTest() {

    private val testSubjectName = data.name
    private val testSubject = data.content

    @Test
    fun `should be enabled by default`() = runComposeTest {
        setContent {
            testSubject(
                Modifier.testTagAsResourceId(testSubjectName),
                TextFieldConfig(
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
                Modifier.testTagAsResourceId(testSubjectName),
                TextFieldConfig(
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
                Modifier.testTagAsResourceId(testSubjectName),
                TextFieldConfig(
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
                Modifier.testTagAsResourceId(testSubjectName),
                TextFieldConfig(
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
                Modifier.testTagAsResourceId(testSubjectName),
                TextFieldConfig(
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
                Modifier.testTagAsResourceId(testSubjectName),
                TextFieldConfig(
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
        fun data(): List<CommonTextFieldTestData> = listOf(
            CommonTextFieldTestData(
                name = "TextFieldOutlined",
                content = { modifier, config ->
                    if (config.isEnabled != null) {
                        TextFieldOutlined(
                            value = "",
                            onValueChange = {},
                            modifier = modifier,
                            label = config.label,
                            isEnabled = config.isEnabled,
                            isReadOnly = config.isReadOnly,
                            isRequired = config.isRequired,
                        )
                    } else {
                        TextFieldOutlined(
                            value = "",
                            onValueChange = {},
                            modifier = modifier,
                            label = config.label,
                            isRequired = config.isRequired,
                            isReadOnly = config.isReadOnly,
                        )
                    }
                },
            ),
            CommonTextFieldTestData(
                name = "TextFieldOutlinedPassword",
                content = { modifier, config ->
                    if (config.isEnabled != null) {
                        TextFieldOutlinedPassword(
                            value = "",
                            onValueChange = {},
                            modifier = modifier,
                            label = config.label,
                            isEnabled = config.isEnabled,
                            isReadOnly = config.isReadOnly,
                            isRequired = config.isRequired,
                        )
                    } else {
                        TextFieldOutlinedPassword(
                            value = "",
                            onValueChange = {},
                            label = config.label,
                            modifier = modifier,
                            isRequired = config.isRequired,
                            isReadOnly = config.isReadOnly,
                        )
                    }
                },
            ),
            CommonTextFieldTestData(
                name = "TextFieldOutlinedEmail",
                content = { modifier, config ->
                    if (config.isEnabled != null) {
                        TextFieldOutlinedEmailAddress(
                            value = "",
                            onValueChange = {},
                            modifier = modifier,
                            label = config.label,
                            isEnabled = config.isEnabled,
                            isReadOnly = config.isReadOnly,
                            isRequired = config.isRequired,
                        )
                    } else {
                        TextFieldOutlinedEmailAddress(
                            value = "",
                            onValueChange = {},
                            modifier = modifier,
                            label = config.label,
                            isRequired = config.isRequired,
                            isReadOnly = config.isReadOnly,
                        )
                    }
                },
            ),
            CommonTextFieldTestData(
                name = "TextFieldOutlinedNumber",
                content = { modifier, config ->
                    if (config.isEnabled != null) {
                        TextFieldOutlinedNumber(
                            value = 123L,
                            onValueChange = {},
                            modifier = modifier,
                            label = config.label,
                            isEnabled = config.isEnabled,
                            isReadOnly = config.isReadOnly,
                            isRequired = config.isRequired,
                        )
                    } else {
                        TextFieldOutlinedNumber(
                            value = 123L,
                            onValueChange = {},
                            modifier = modifier,
                            label = config.label,
                            isRequired = config.isRequired,
                            isReadOnly = config.isReadOnly,
                        )
                    }
                },
            ),
            CommonTextFieldTestData(
                name = "TextFieldOutlinedSelect",
                content = { modifier, config ->
                    if (config.isEnabled != null) {
                        TextFieldOutlinedSelect(
                            options = persistentListOf("option1", "option2"),
                            selectedOption = "option1",
                            onValueChange = {},
                            modifier = modifier,
                            label = config.label,
                            isEnabled = config.isEnabled,
                            isReadOnly = config.isReadOnly,
                            isRequired = config.isRequired,
                        )
                    } else {
                        TextFieldOutlinedSelect(
                            options = persistentListOf("option1", "option2"),
                            selectedOption = "option1",
                            onValueChange = {},
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

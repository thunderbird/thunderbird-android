package net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithText
import app.k9mail.core.ui.compose.testing.onNodeWithTextIgnoreCase
import app.k9mail.core.ui.compose.testing.pressBack
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.R as DialogR

class TextDialogViewTest : ComposeTest() {

    @Test
    fun `should display title description and initial value`() = runComposeTest {
        // Arrange
        val setting = SettingValue.Text(
            id = "text_setting",
            title = { "Title" },
            description = { "Description" },
            value = "Initial",
        )

        // Act
        setContentWithTheme {
            TextDialogView(
                setting = setting,
                onConfirmClick = {},
                onDismissClick = {},
                onDismissRequest = {},
            )
        }

        // Assert
        onNodeWithText("Title").assertExists()
        onNodeWithText("Description").assertExists()
        onNodeWithText("Initial").assertExists()
    }

    @Test
    fun `should apply transform on text change`() = runComposeTest {
        // Arrange
        val setting = SettingValue.Text(
            id = "text_setting",
            title = { "Title" },
            description = { null },
            value = "start",
            transform = { it.uppercase() },
        )

        // Act
        setContentWithTheme {
            TextDialogView(
                setting = setting,
                onConfirmClick = {},
                onDismissClick = {},
                onDismissRequest = {},
            )
        }
        onNodeWithText("start").assertExists()
        onNodeWithText("start").performTextClearance()
        onNode(hasSetTextAction()).performTextInput("abc")

        // Assert
        onNodeWithText("ABC").assertExists()
    }

    @Test
    fun `should call onConfirmClick with updated value`() = runComposeTest {
        // Arrange
        val setting = SettingValue.Text(
            id = "text_setting",
            title = { "Title" },
            description = { null },
            value = "foo",
            transform = { it },
        )
        var confirmed: SettingValue<*>? = null

        // Act
        setContentWithTheme {
            TextDialogView(
                setting = setting,
                onConfirmClick = { confirmed = it },
                onDismissClick = {},
                onDismissRequest = {},
            )
        }
        onNodeWithText("foo").performTextClearance()
        onNode(hasSetTextAction()).performTextInput("bar")
        onNodeWithTextIgnoreCase(DialogR.string.core_ui_setting_dialog_button_accept).performClick()

        // Assert
        val result = confirmed as SettingValue.Text
        assertThat(result.value).isEqualTo("bar")
    }

    @Test
    fun `should call onDismissClick callback`() = runComposeTest {
        // Arrange
        val setting = SettingValue.Text(
            id = "text_setting",
            title = { "Title" },
            description = { null },
            value = "value",
        )
        var dismissedClick = false

        // Act
        setContentWithTheme {
            TextDialogView(
                setting = setting,
                onConfirmClick = {},
                onDismissClick = { dismissedClick = true },
                onDismissRequest = {},
            )
        }
        onNodeWithTextIgnoreCase(DialogR.string.core_ui_setting_dialog_button_cancel).performClick()

        // Assert
        assertThat(dismissedClick).isTrue()
    }

    @Test
    fun `should call onDismissRequest callback on back press`() = runComposeTest {
        // Arrange
        val setting = SettingValue.Text(
            id = "text_setting",
            title = { "Title" },
            description = { null },
            value = "value",
        )
        var dismissedRequest = false

        // Act
        setContentWithTheme {
            TextDialogView(
                setting = setting,
                onConfirmClick = {},
                onDismissClick = {},
                onDismissRequest = { dismissedRequest = true },
            )
        }
        pressBack()

        // Assert
        assertThat(dismissedRequest).isTrue()
    }
}

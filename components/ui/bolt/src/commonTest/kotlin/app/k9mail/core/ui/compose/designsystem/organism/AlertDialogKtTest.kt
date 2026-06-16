package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import assertk.assertThat
import assertk.assertions.isTrue
import kotlin.test.Test
import net.thunderbird.core.ui.compose.designsystem.setContentWithTheme
import net.thunderbird.components.ui.testing.ComposeUiTestHarness

class AlertDialogKtTest : ComposeUiTestHarness() {

    @Test
    fun `should display title, text and confirm button`() = runComposeTest {
        setContentWithTheme {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmText = "Confirm",
                onConfirmClick = {},
                onDismissRequest = {},
            )
        }

        onNodeWithText("Title").assertExists()
        onNodeWithText("Text").assertExists()
        onNodeWithTextIgnoreCase(text = "Confirm").assertExists()
    }

    @Test
    fun `should call onConfirmClick when confirm button is clicked`() = runComposeTest {
        var clicked = false
        setContentWithTheme {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmText = "Confirm",
                onConfirmClick = { clicked = true },
                onDismissRequest = {},
            )
        }

        onNodeWithTextIgnoreCase(text = "Confirm").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `should display dismiss button and call onDismissClick when clicked`() = runComposeTest {
        var clicked = false
        setContentWithTheme {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmText = "Confirm",
                onConfirmClick = {},
                onDismissClick = {
                    clicked = true
                },
                onDismissRequest = {},
                dismissText = "Dismiss",
            )
        }

        onNodeWithTextIgnoreCase(text = "Dismiss").assertExists()
        onNodeWithTextIgnoreCase(text = "Dismiss").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `should call onDismissRequest when dialog is dismissed`() = runComposeTest {
        var dismissed = false
        setContentWithTheme {
            Column {
                TextTitleMedium("Other")
                AlertDialog(
                    title = "Title",
                    text = "Text",
                    confirmText = "Confirm",
                    onConfirmClick = {},
                    onDismissRequest = { dismissed = true },
                )
            }
        }

        pressBack()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun `should contain custom content`() = runComposeTest {
        setContentWithTheme {
            AlertDialog(
                title = "Title",
                confirmText = "Confirm",
                onConfirmClick = {},
                onDismissRequest = {},
            ) {
                Column {
                    TextTitleMedium("Custom")
                }
            }
        }

        onNodeWithText("Custom").assertExists()
    }
}

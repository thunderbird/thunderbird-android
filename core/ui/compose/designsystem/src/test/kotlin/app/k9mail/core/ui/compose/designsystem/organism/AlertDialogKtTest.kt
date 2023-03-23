package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithTextIgnoreCase
import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.Test

class AlertDialogKtTest : ComposeTest() {

    @Test
    fun `should display title, text and confirmButton by default`() = runComposeTest {
        setContent {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = {},
                onDismissRequest = {},
            )
        }

        onNodeWithText("Title").assertExists()
        onNodeWithText("Text").assertExists()
        onNodeWithTextIgnoreCase(text = "Confirm").assertExists()
    }

    @Test
    fun `should call onConfirmButtonClick when confirmButton is clicked`() = runComposeTest {
        var clicked = false
        setContent {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = { clicked = true },
                onDismissRequest = {},
            )
        }

        onNodeWithTextIgnoreCase(text = "Confirm").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `should display dismissButton when dismissButtonText is set`() = runComposeTest {
        setContent {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = {},
                onDismissRequest = {},
                dismissButtonText = "Dismiss",
            )
        }

        onNodeWithTextIgnoreCase(text = "Dismiss").assertExists()
    }

    @Test
    fun `should call onDismissButtonClick when dismissButton is clicked`() = runComposeTest {
        var clicked = false
        setContent {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = {},
                onDismissRequest = { clicked = true },
                dismissButtonText = "Dismiss",
                onDismissButtonClick = { clicked = true },
            )
        }

        onNodeWithTextIgnoreCase(text = "Dismiss").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `should call onDismissRequest when back pressed`() = runComposeTest {
        var dismissed = false
        setContent {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = {},
                onDismissRequest = { dismissed = true },
                dismissButtonText = "Dismiss",
                modifier = Modifier.testTag("AlertDialog"),
            )
        }

        Espresso.pressBack()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun `should display success icon when type success and titleIcon is enabled`() = runComposeTest {
        setContent {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = {},
                onDismissRequest = {},
                hasTitleIcon = true,
                type = AlertDialogType.Success,
            )
        }

        onNodeWithContentDescription(
            getString(R.string.designsystem_atom_icon_success),
        ).assertExists()
    }

    @Test
    fun `should display error icon when type error and titleIcon is enabled`() = runComposeTest {
        setContent {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = {},
                onDismissRequest = {},
                hasTitleIcon = true,
                type = AlertDialogType.Error,
            )
        }

        onNodeWithContentDescription(
            getString(R.string.designsystem_atom_icon_error),
        ).assertExists()
    }

    @Test
    fun `should display warning icon when type warning and titleIcon is enabled`() = runComposeTest {
        setContent {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = {},
                onDismissRequest = {},
                hasTitleIcon = true,
                type = AlertDialogType.Warning,
            )
        }

        onNodeWithContentDescription(
            getString(R.string.designsystem_atom_icon_warning),
        ).assertExists()
    }

    @Test
    fun `should display info icon when type info and titleIcon is enabled`() = runComposeTest {
        setContent {
            AlertDialog(
                title = "Title",
                text = "Text",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = {},
                onDismissRequest = {},
                hasTitleIcon = true,
                type = AlertDialogType.Info,
            )
        }

        onNodeWithContentDescription(
            getString(R.string.designsystem_atom_icon_info),
        ).assertExists()
    }
}

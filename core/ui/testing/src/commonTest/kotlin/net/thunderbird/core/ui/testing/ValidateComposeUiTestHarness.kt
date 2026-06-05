package net.thunderbird.core.ui.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import assertk.assertThat
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class ValidateComposeUiTestHarness : ComposeUiTestHarness() {

    @Test
    fun `setContent should set the content for Compose UI test`() = runComposeTest {
        setContent {
            Column {
                BasicText("Hello, Compose!", modifier = Modifier.testTag("text"))
            }
        }

        onNodeWithTag("text").assertExists()
    }

    @Test
    fun `onNodeWithTag should find node with tag`() = runComposeTest {
        setContent {
            Column {
                BasicText("Hello, Compose!", modifier = Modifier.testTag("text"))
            }
        }

        onNodeWithTag("text").assertExists()
        onNodeWithTag("text", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `onNodeWithText should find node with text`() = runComposeTest {
        setContent {
            BasicText("Hello, Compose!")
        }

        onNodeWithText("Hello, Compose!").assertExists()
        onNodeWithText("Hello", substring = true).assertExists()
        onNodeWithText("HELLO, COMPOSE!", ignoreCase = true).assertExists()
        onNodeWithText("Hello, Compose!", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `onNodeWithTextIgnoreCase should find node ignoring case`() = runComposeTest {
        setContent {
            BasicText("Hello, Compose!")
        }

        onNodeWithTextIgnoreCase("HELLO, COMPOSE!").assertExists()
        onNodeWithTextIgnoreCase("HELLO", substring = true).assertExists()
        onNodeWithTextIgnoreCase("HELLO, COMPOSE!", useUnmergedTree = true).assertExists()
    }
}

package net.thunderbird.core.ui.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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

    @Test
    fun `setContentWithWindowSize should set local window info`() = runComposeTest {
        setContentWithWindowSize(windowSize = DpSize(width = 456.dp, height = 789.dp)) {
            val density = LocalDensity.current
            val windowInfo = LocalWindowInfo.current
            val windowSize = with(density) {
                DpSize(
                    width = windowInfo.containerSize.width.toDp(),
                    height = windowInfo.containerSize.height.toDp(),
                )
            }

            BasicText(
                text = if (windowSize == DpSize(width = 456.dp, height = 789.dp)) {
                    "Window size matches"
                } else {
                    "Window size does not match"
                },
            )
        }

        onNodeWithText("Window size matches").assertExists()
    }
}

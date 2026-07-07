package net.thunderbird.components.ui.testing

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.test.Test
import kotlin.test.assertTrue

class JvmComposeUiTestScopeTest : ComposeUiTestHarness() {

    @Test
    fun `pressBack should send Escape key input`() = runComposeTest {
        var escapePressed = false

        setContent {
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Box(
                modifier = Modifier
                    .testTag(TargetTag)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                            escapePressed = true
                            true
                        } else {
                            false
                        }
                    },
            )
        }
        waitForIdle()

        pressBack()

        assertTrue(escapePressed)
    }

    @Test
    fun `pressBack should perform dismiss action on active dialog`() = runComposeTest {
        var dismissed = false

        setContent {
            Dialog(
                onDismissRequest = {
                    dismissed = true
                },
            ) {
                Box(modifier = Modifier.size(1.dp))
            }
        }
        waitForIdle()

        pressBack()

        assertTrue(dismissed)
    }

    private companion object {
        const val TargetTag = "target"
    }
}

package net.thunderbird.core.ui.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.findDefaultNavigationEventDispatcherOwner
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.navigationevent.NavigationEventInput

/**
 * A scope for Compose UI tests on JVM.
 *
 * It delegates scope calls to the ComposeUiTest instance.
 *
 * @param delegate the ComposeUiTest instance to delegate to
 */
@OptIn(ExperimentalTestApi::class, InternalComposeApi::class)
internal class JvmComposeUiTestScope(
    private val delegate: ComposeUiTest,
) : ComposeUiTestScope {
    private var backInput: TestBackInput? = null

    override fun setContent(content: @Composable (() -> Unit)) {
        delegate.setContent {
            val navigationEventDispatcher = findDefaultNavigationEventDispatcherOwner()?.navigationEventDispatcher

            DisposableEffect(navigationEventDispatcher) {
                if (navigationEventDispatcher == null) {
                    return@DisposableEffect onDispose {
                        backInput = null
                    }
                }

                val input = TestBackInput()
                navigationEventDispatcher.addInput(input)
                backInput = input

                onDispose {
                    navigationEventDispatcher.removeInput(input)
                    if (backInput == input) {
                        backInput = null
                    }
                }
            }

            content()
        }
    }

    override fun onNodeWithTag(tag: String, useUnmergedTree: Boolean) =
        delegate.onNodeWithTag(tag, useUnmergedTree)

    override fun onNodeWithText(
        text: String,
        substring: Boolean,
        ignoreCase: Boolean,
        useUnmergedTree: Boolean,
    ) = delegate.onNodeWithText(text, substring, ignoreCase, useUnmergedTree)

    override fun onNodeWithTextIgnoreCase(
        text: String,
        substring: Boolean,
        useUnmergedTree: Boolean,
    ) = delegate.onNodeWithText(text, substring, true, useUnmergedTree)

    override fun onNodeWithContentDescription(
        label: String,
        substring: Boolean,
        ignoreCase: Boolean,
        useUnmergedTree: Boolean,
    ) = delegate.onNodeWithContentDescription(label, substring, ignoreCase, useUnmergedTree)

    override fun waitForIdle() {
        delegate.waitForIdle()
    }

    override fun pressBack() {
        val input = backInput
        if (input?.hasEnabledHandlers == true) {
            delegate.runOnUiThread {
                input.pressBack()
            }
            delegate.waitForIdle()
            return
        }

        val roots = delegate.onAllNodes(isRoot())
        val activeRootIndex = roots.fetchSemanticsNodes().lastIndex

        roots[activeRootIndex].performKeyInput {
            pressKey(Key.Escape)
        }
    }

    private class TestBackInput : NavigationEventInput() {
        var hasEnabledHandlers: Boolean = false
            private set

        override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
            this.hasEnabledHandlers = hasEnabledHandlers
        }

        fun pressBack() {
            dispatchOnBackCompleted()
        }
    }
}

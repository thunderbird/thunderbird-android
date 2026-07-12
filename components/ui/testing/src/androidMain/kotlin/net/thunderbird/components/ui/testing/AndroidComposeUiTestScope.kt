package net.thunderbird.components.ui.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso

/**
 * A scope for Compose UI tests on Android.
 *
 * It delegates scope calls to the ComposeUiTest instance.
 *
 * @param delegate the ComposeUiTest instance to delegate to
 */
@OptIn(ExperimentalTestApi::class)
internal class AndroidComposeUiTestScope(
    private val delegate: ComposeUiTest,
) : ComposeUiTestScope {
    override fun setContent(content: @Composable () -> Unit) {
        delegate.setContent(content)
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

    override fun pressBack() = Espresso.pressBack()
}

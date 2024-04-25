@file:Suppress("TooManyFunctions")

package app.k9mail.core.ui.compose.testing

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import app.k9mail.core.ui.compose.theme2.k9mail.K9MailTheme2
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
open class ComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    fun getString(@StringRes resourceId: Int): String = RuntimeEnvironment.getApplication().getString(resourceId)

    fun runComposeTest(testContent: ComposeContentTestRule.() -> Unit) = with(composeTestRule) {
        testContent()
    }
}

/**
 * Set the content of the test
 */
fun ComposeTest.setContent(content: @Composable () -> Unit) = composeTestRule.setContent(content)

/**
 * Set the content of the test and wrap it in the default theme.
 */
fun ComposeTest.setContentWithTheme(content: @Composable () -> Unit) = composeTestRule.setContent {
    K9MailTheme2 {
        content()
    }
}

fun ComposeTest.onNodeWithTag(
    tag: String,
    useUnmergedTree: Boolean = false,
) = composeTestRule.onNodeWithTag(tag, useUnmergedTree)

fun ComposeTest.onAllNodesWithTag(
    tag: String,
    useUnmergedTree: Boolean = false,
) = composeTestRule.onAllNodesWithTag(tag, useUnmergedTree)

fun ComposeTest.onNodeWithContentDescription(
    label: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false,
) = composeTestRule.onNodeWithContentDescription(label, substring, ignoreCase, useUnmergedTree)

fun ComposeTest.onNodeWithText(
    text: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false,
) = composeTestRule.onNodeWithText(text, substring, ignoreCase, useUnmergedTree)

fun ComposeTest.onNodeWithText(
    @StringRes resourceId: Int,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false,
) = composeTestRule.onNodeWithText(getString(resourceId), substring, ignoreCase, useUnmergedTree)

fun ComposeTest.onNodeWithTextIgnoreCase(
    text: String,
    substring: Boolean = false,
    useUnmergedTree: Boolean = false,
) = composeTestRule.onNodeWithText(text, substring, true, useUnmergedTree)

fun ComposeTest.onNodeWithTextIgnoreCase(
    @StringRes resourceId: Int,
    substring: Boolean = false,
    useUnmergedTree: Boolean = false,
) = composeTestRule.onNodeWithText(getString(resourceId), substring, true, useUnmergedTree)

fun ComposeTest.onAllNodesWithText(
    text: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false,
) = composeTestRule.onAllNodesWithText(text, substring, ignoreCase, useUnmergedTree)

fun ComposeTest.onAllNodesWithContentDescription(
    label: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false,
) = composeTestRule.onAllNodesWithContentDescription(label, substring, ignoreCase, useUnmergedTree)

fun ComposeTest.onRoot(useUnmergedTree: Boolean = false) = composeTestRule.onRoot(useUnmergedTree)

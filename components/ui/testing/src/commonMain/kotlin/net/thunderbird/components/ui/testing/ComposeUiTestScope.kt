package net.thunderbird.components.ui.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection

/**
 * A scope for Compose UI tests. It provides a platform agnostic way to use ComposeUiTest API.
 */
public interface ComposeUiTestScope {

    /**
     * Sets the content of the Compose UI test scope.
     */
    public fun setContent(content: @Composable () -> Unit)

    /**
     * Find a node with the given [tag].
     *
     * @param tag The tag of the node to find.
     * @param useUnmergedTree Whether to use the unmerged tree.
     * @return A [SemanticsNodeInteractionCollection] containing the found nodes.
     */
    public fun onNodeWithTag(
        tag: String,
        useUnmergedTree: Boolean = false,
    ): SemanticsNodeInteraction

    /**
     * Find a node with the given [text].
     *
     * @param text The text of the node to find.
     * @param substring Whether to search for a substring of the text.
     * @param ignoreCase Whether to ignore case when searching for the text.
     * @param useUnmergedTree Whether to use the unmerged tree.
     * @return A [SemanticsNodeInteractionCollection] containing the found nodes.
     */
    public fun onNodeWithText(
        text: String,
        substring: Boolean = false,
        ignoreCase: Boolean = false,
        useUnmergedTree: Boolean = false,
    ): SemanticsNodeInteraction

    /**
     * Find a node with the given [text] ignoring case.
     *
     * @param text The text of the node to find.
     * @param substring Whether to search for a substring of the text.
     * @param useUnmergedTree Whether to use the unmerged tree.
     * @return A [List<SemanticsNodeInteractionCollection>] containing the found nodes.
     */
    public fun onNodeWithTextIgnoreCase(
        text: String,
        substring: Boolean = false,
        useUnmergedTree: Boolean = false,
    ): SemanticsNodeInteraction

    /**
     * Find a node with the given [label].
     *
     * @param label The content description of the node to find.
     * @param substring Whether to search for a substring of the content description.
     * @param ignoreCase Whether to ignore case when searching for the content description.
     * @param useUnmergedTree Whether to use the unmerged tree.
     * @return A [SemanticsNodeInteraction] containing the found node.
     */
    public fun onNodeWithContentDescription(
        label: String,
        substring: Boolean = false,
        ignoreCase: Boolean = false,
        useUnmergedTree: Boolean = false,
    ): SemanticsNodeInteraction

    /**
     * Waits for the Compose UI to be idle.
     */
    public fun waitForIdle()

    /**
     *  Presses the back button.
     */
    public fun pressBack()
}

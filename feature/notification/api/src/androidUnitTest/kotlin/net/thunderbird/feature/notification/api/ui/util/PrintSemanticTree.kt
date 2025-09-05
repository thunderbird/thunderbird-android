package net.thunderbird.feature.notification.api.ui.util

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import app.k9mail.core.ui.compose.testing.ComposeTest

internal fun ComposeTest.printSemanticTree(
    root: SemanticsNodeInteraction = composeTestRule.onRoot(useUnmergedTree = true),
) {
    println("-----")
    println("Semantic tree:")
    println(root.printToString())
    println("-----")
    println()
}

package app.k9mail.core.ui.compose.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.printToString

fun SemanticsNodeInteractionsProvider.printViewTree(node: SemanticsNodeInteraction) = println(node.printToString())

fun SemanticsNodeInteractionsProvider.onNodeWithTextIgnoreCase(text: String) = onNodeWithText(text, ignoreCase = true)

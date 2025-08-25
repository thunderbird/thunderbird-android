package net.thunderbird.core.ui.compose.common.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Adds a test tag to the element with testTagsAsResourceId set to true.
 * This allows the element to be found by its test tag during UI testing.
 *
 * @param tag The test tag to be assigned to the element.
 * @return A [Modifier] with the test tag applied.
 */
fun Modifier.testTagAsResourceId(tag: String): Modifier = this
    .semantics { testTagsAsResourceId = true }
    .testTag(tag)

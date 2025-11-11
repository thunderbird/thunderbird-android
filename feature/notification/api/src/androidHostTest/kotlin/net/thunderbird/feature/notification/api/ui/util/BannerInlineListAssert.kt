package net.thunderbird.feature.notification.api.ui.util

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import app.k9mail.core.ui.compose.designsystem.organism.banner.BannerNotificationCardDefaults.TEST_TAG_BANNER_INLINE_CARD_ACTION_ROW
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithTag
import net.thunderbird.feature.notification.api.ui.BannerInlineNotificationListHostDefaults

internal fun ComposeTest.assertBannerInlineList(
    size: Int,
    assertListHost: SemanticsNodeInteraction.() -> Unit = { assertIsDisplayed() },
) {
    val listHost = onNodeWithTag(
        BannerInlineNotificationListHostDefaults.TEST_TAG_BANNER_INLINE_LIST,
        useUnmergedTree = true,
    )

    listHost
        .onChildren()
        .assertCountEquals(size)

    listHost.assertListHost()
}

internal fun SemanticsNodeInteraction.assertBannerInline(
    index: Int,
    title: String,
    supportingText: String,
    assertActions: SemanticsNodeInteractionCollection.() -> Unit = {},
) {
    val banner = onChildAt(index)
    val children = banner.onChildren()
    children
        .filterToOne(hasTextExactly(title))
        .assertIsDisplayed()

    children
        .filterToOne(hasTextExactly(supportingText))
        .assertIsDisplayed()

    children
        .filterToOne(hasTestTag(TEST_TAG_BANNER_INLINE_CARD_ACTION_ROW))
        .onChildren()
        .assertActions()
}

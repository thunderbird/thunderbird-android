package net.thunderbird.components.ui.catalog.ui.page.organism.items.banners

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.molecule.notification.NotificationActionButton
import net.thunderbird.components.ui.bolt.organism.banner.inline.ErrorBannerInlineNotificationCard
import net.thunderbird.components.ui.bolt.organism.banner.inline.InfoBannerInlineNotificationCard
import net.thunderbird.components.ui.bolt.organism.banner.inline.SuccessBannerInlineNotificationCard
import net.thunderbird.components.ui.bolt.organism.banner.inline.WarningBannerInlineNotificationCard
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionSubtitleItem

fun LazyGridScope.bannerInline() {
    sectionHeaderItem("Banner Inline")
    errorBannerInline()
    infoBannerInline()
    warningBannerInline()
    successBannerInline()
}

fun LazyGridScope.errorBannerInline() {
    sectionSubtitleItem("Error")
    fullSpanItem {
        ErrorBannerInlineNotificationCard(
            title = "Notification title",
            supportingText = "Supporting text",
            actions = {
                NotificationActionButton(text = "View support article", onClick = {}, isExternalLink = true)
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            modifier = Modifier.padding(BoltTheme.spacings.double),
        )
    }
}

fun LazyGridScope.infoBannerInline() {
    sectionSubtitleItem("Information")
    fullSpanItem {
        InfoBannerInlineNotificationCard(
            title = "Notification title",
            supportingText = "Supporting text",
            actions = {
                NotificationActionButton(text = "Action 2", onClick = {})
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            modifier = Modifier.padding(BoltTheme.spacings.double),
        )
    }
}

fun LazyGridScope.warningBannerInline() {
    sectionSubtitleItem("Warning")
    fullSpanItem {
        WarningBannerInlineNotificationCard(
            title = "Notification title",
            supportingText = "Supporting text",
            actions = {
                NotificationActionButton(text = "Action 2", onClick = {})
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            modifier = Modifier.padding(BoltTheme.spacings.double),
        )
    }
}

fun LazyGridScope.successBannerInline() {
    sectionSubtitleItem("Success")
    fullSpanItem {
        SuccessBannerInlineNotificationCard(
            title = "Notification title",
            supportingText = "Supporting text",
            actions = {
                NotificationActionButton(text = "Action 2", onClick = {})
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            modifier = Modifier.padding(BoltTheme.spacings.double),
        )
    }
}

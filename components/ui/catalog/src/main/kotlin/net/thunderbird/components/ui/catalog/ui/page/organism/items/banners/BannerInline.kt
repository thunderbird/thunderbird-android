package net.thunderbird.components.ui.catalog.ui.page.organism.items.banners

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.molecule.notification.NotificationActionButton
import net.thunderbird.components.ui.bolt.organism.banner.global.ErrorBannerGlobalNotificationCard
import net.thunderbird.components.ui.bolt.organism.banner.global.InfoBannerGlobalNotificationCard
import net.thunderbird.components.ui.bolt.organism.banner.global.SuccessBannerGlobalNotificationCard
import net.thunderbird.components.ui.bolt.organism.banner.global.WarningBannerGlobalNotificationCard
import net.thunderbird.components.ui.bolt.theme.MainTheme
import net.thunderbird.components.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionSubtitleItem

fun LazyGridScope.bannerGlobal() {
    sectionHeaderItem("Banner Global")
    errorBannerGlobal()
    infoBannerGlobal()
    warningBannerGlobal()
    successBannerGlobal()
}

fun LazyGridScope.errorBannerGlobal() {
    sectionSubtitleItem("Error")
    fullSpanItem {
        ErrorBannerGlobalNotificationCard(
            text = "Notification Text",
            action = {
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            modifier = Modifier.padding(MainTheme.spacings.double),
        )
    }
}

fun LazyGridScope.infoBannerGlobal() {
    sectionSubtitleItem("Information")
    fullSpanItem {
        InfoBannerGlobalNotificationCard(
            text = "Notification Text",
            action = {
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            modifier = Modifier.padding(MainTheme.spacings.double),
        )
    }
}

fun LazyGridScope.warningBannerGlobal() {
    sectionSubtitleItem("Warning")
    fullSpanItem {
        WarningBannerGlobalNotificationCard(
            text = "Notification Text",
            action = {
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            modifier = Modifier.padding(MainTheme.spacings.double),
        )
    }
}

fun LazyGridScope.successBannerGlobal() {
    sectionSubtitleItem("Success")
    fullSpanItem {
        SuccessBannerGlobalNotificationCard(
            text = "Notification Text",
            action = {
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            modifier = Modifier.padding(MainTheme.spacings.double),
        )
    }
}

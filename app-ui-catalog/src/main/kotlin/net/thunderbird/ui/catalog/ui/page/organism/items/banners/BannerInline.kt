package net.thunderbird.ui.catalog.ui.page.organism.items.banners

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.molecule.notification.NotificationActionButton
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.ErrorBannerGlobalNotificationCard
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.InfoBannerGlobalNotificationCard
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.SuccessBannerGlobalNotificationCard
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.WarningBannerGlobalNotificationCard
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

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

package com.fsck.k9.ui.messagelist.item

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import kotlinx.collections.immutable.persistentSetOf
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.notification.api.receiver.InAppNotificationEvent
import net.thunderbird.feature.notification.api.ui.InAppNotificationHost
import net.thunderbird.feature.notification.api.ui.host.DisplayInAppNotificationFlag
import org.koin.compose.koinInject

abstract class MessageListInAppNotificationViewHolder(protected val view: ComposeView) : MessageListViewHolder(view) {
    fun bind() {
        view.setContent {
            val themeProvider = koinInject<FeatureThemeProvider>()
            themeProvider.WithTheme {
                Content()
            }
        }
    }

    @Composable
    abstract fun Content()
}

class BannerInlineListInAppNotificationViewHolder(
    private val eventFilter: (InAppNotificationEvent) -> Boolean,
    view: ComposeView,
) : MessageListInAppNotificationViewHolder(view) {
    @Composable
    override fun Content() {
        InAppNotificationHost(
            onActionClick = { },
            enabled = persistentSetOf(DisplayInAppNotificationFlag.BannerInlineNotifications),
            eventFilter = eventFilter,
            modifier = Modifier.animateContentSize(),
        )
    }
}

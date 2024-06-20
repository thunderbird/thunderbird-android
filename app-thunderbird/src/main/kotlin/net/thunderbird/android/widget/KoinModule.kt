package net.thunderbird.android.widget

import app.k9mail.feature.widget.message.list.MessageListWidgetConfig
import app.k9mail.feature.widget.unread.UnreadWidgetConfig
import org.koin.dsl.module

val appWidgetModule = module {
    single<MessageListWidgetConfig> { TbMessageListWidgetConfig() }
    single<UnreadWidgetConfig> { TbUnreadWidgetConfig() }
}

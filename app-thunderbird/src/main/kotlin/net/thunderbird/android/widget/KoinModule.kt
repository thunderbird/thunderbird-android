package net.thunderbird.android.widget

import app.k9mail.feature.widget.message.list.MessageListWidgetConfig
import app.k9mail.feature.widget.unread.UnreadWidgetClassProvider
import net.thunderbird.android.widget.provider.UnreadWidgetProvider
import org.koin.dsl.module

val appWidgetModule = module {
    single<MessageListWidgetConfig> { TbMessageListWidgetConfig() }

    single<UnreadWidgetClassProvider> {
        UnreadWidgetClassProvider { UnreadWidgetProvider::class.java }
    }
}

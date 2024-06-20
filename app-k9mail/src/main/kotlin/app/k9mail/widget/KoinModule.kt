package app.k9mail.widget

import app.k9mail.feature.widget.message.list.MessageListWidgetConfig
import app.k9mail.feature.widget.unread.UnreadWidgetClassProvider
import com.fsck.k9.provider.UnreadWidgetProvider
import org.koin.dsl.module

val appWidgetModule = module {
    single<MessageListWidgetConfig> { K9MessageListWidgetConfig() }

    single<UnreadWidgetClassProvider> {
        UnreadWidgetClassProvider { UnreadWidgetProvider::class.java }
    }
}

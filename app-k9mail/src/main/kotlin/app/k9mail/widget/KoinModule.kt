package app.k9mail.widget

import app.k9mail.feature.widget.message.list.MessageListWidgetConfig
import app.k9mail.feature.widget.unread.UnreadWidgetConfig
import net.thunderbird.feature.widget.message.list.featureWidgetMessageListModule
import org.koin.dsl.module

val appWidgetModule = module {
    includes(featureWidgetMessageListModule)

    single<MessageListWidgetConfig> { K9MessageListWidgetConfig() }
    single<UnreadWidgetConfig> { K9UnreadWidgetConfig() }
}

package net.thunderbird.android.widget

import app.k9mail.feature.widget.message.list.MessageListWidgetConfig
import app.k9mail.feature.widget.unread.UnreadWidgetConfig
import net.thunderbird.feature.widget.message.list.featureWidgetMessageListGlanceDiModule
import org.koin.dsl.module

internal val widgetDiModule = module {
    includes(featureWidgetMessageListGlanceDiModule)

    single<MessageListWidgetConfig> { TbMessageListWidgetConfig() }
    single<UnreadWidgetConfig> { TbUnreadWidgetConfig() }
}

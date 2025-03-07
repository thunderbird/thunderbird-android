package app.k9mail.feature.widget.message.glance

import org.koin.dsl.module

val messageListGlanceWidgetModule = module {
    factory { MessageListLoader(preferences = get(), messageListRepository = get(), messageHelper = get()) }
}

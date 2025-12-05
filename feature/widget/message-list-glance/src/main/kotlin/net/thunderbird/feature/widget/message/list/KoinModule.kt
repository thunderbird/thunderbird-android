package net.thunderbird.feature.widget.message.list

import org.koin.dsl.module

val featureWidgetMessageListModule = module {
    factory {
        MessageListLoader(
            accountManager = get(),
            messageListRepository = get(),
            messageHelper = get(),
            messageListPreferencesManager = get(),
            outboxFolderManager = get(),
        )
    }
}

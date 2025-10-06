package net.thunderbird.feature.widget.message.list

import org.koin.dsl.module

val featureWidgetMessageListModule = module {
    factory {
        MessageListLoader(
            preferences = get(),
            messageListRepository = get(),
            messageHelper = get(),
            generalSettingsManager = get(),
            outboxFolderManager = get(),
        )
    }
}

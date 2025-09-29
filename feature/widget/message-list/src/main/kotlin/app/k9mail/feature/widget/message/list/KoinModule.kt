package app.k9mail.feature.widget.message.list

import org.koin.dsl.module

val messageListWidgetModule = module {
    single { MessageListWidgetManager(context = get(), messageListRepository = get(), config = get()) }
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

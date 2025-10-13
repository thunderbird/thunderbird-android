package net.thunderbird.app.common.feature.mail.message

import net.thunderbird.feature.mail.message.export.DefaultMessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.MessageFileNameSuggester
import org.koin.dsl.module

internal val mailMessageModule = module {
    single<MessageFileNameSuggester> { DefaultMessageFileNameSuggester() }
}

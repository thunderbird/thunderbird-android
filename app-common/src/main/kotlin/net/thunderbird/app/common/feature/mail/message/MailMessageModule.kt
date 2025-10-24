package net.thunderbird.app.common.feature.mail.message

import net.thunderbird.feature.mail.message.export.DefaultMessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.MessageExporter
import net.thunderbird.feature.mail.message.export.MessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.eml.EmlMessageExporter
import org.koin.dsl.module

internal val mailMessageModule = module {
    single<MessageFileNameSuggester> { DefaultMessageFileNameSuggester() }

    single<MessageExporter> {
        EmlMessageExporter(
            fileManager = get(),
        )
    }
}

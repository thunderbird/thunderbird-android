package net.thunderbird.app.composition.feature.mail

import net.thunderbird.feature.mail.message.export.DefaultMessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.MessageExporter
import net.thunderbird.feature.mail.message.export.MessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.eml.EmlMessageExporter
import org.koin.dsl.module

internal val mailCompositionModule = module {
    single<MessageFileNameSuggester> { DefaultMessageFileNameSuggester() }
    single<MessageExporter> { EmlMessageExporter(fileManager = get()) }
}

package net.thunderbird.app.composition.feature.mail

import net.thunderbird.feature.mail.message.export.DefaultMessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.MessageExporter
import net.thunderbird.feature.mail.message.export.MessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.eml.EmlMessageExporter
import net.thunderbird.feature.mail.message.reader.impl.inject.featureMessageReaderModule
import org.koin.dsl.module

internal val mailCompositionModule = module {
    includes(featureMessageReaderModule)

    single<MessageFileNameSuggester> { DefaultMessageFileNameSuggester() }
    single<MessageExporter> { EmlMessageExporter(fileManager = get()) }
}

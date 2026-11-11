package com.fsck.k9.message.quote

import org.koin.dsl.module

val quoteDiModule = module {
    factory { QuoteDateFormatter(generalSettingsManager = get()) }
    factory { TextQuoteCreator(get(), get()) }
}

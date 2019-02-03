package com.fsck.k9.message.quote

import org.koin.dsl.module.module

val quoteModule = module {
    factory { QuoteHelper(get()) }
    factory { TextQuoteCreator(get(), get()) }
}

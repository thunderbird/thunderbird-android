package com.fsck.k9.message.quote

import org.koin.dsl.module

val quoteModule = module {
    factory { QuoteDateFormatter() }
    factory { TextQuoteCreator(get(), get()) }
}

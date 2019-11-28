package com.fsck.k9.message.html

import org.koin.dsl.module

val htmlModule = module {
    single { HtmlProcessorFactory(get(), get()) }
    single { HtmlSanitizer() }
    single { DisplayHtmlFactory() }
}

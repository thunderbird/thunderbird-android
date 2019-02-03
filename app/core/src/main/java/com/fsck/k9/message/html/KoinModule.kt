package com.fsck.k9.message.html

import org.koin.dsl.module.module

val htmlModule = module {
    single { HtmlProcessor(get()) }
    single { HtmlSanitizer() }
}

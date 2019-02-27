package com.fsck.k9.message.extractors

import org.koin.dsl.module.module

val extractorModule = module {
    single { AttachmentInfoExtractor(get()) }
}

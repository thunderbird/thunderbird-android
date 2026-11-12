package com.fsck.k9.message.extractors

import org.koin.dsl.module

internal val extractorDiModule = module {
    single { AttachmentInfoExtractor(get()) }
    single { TextPartFinder() }
    single { BasicPartInfoExtractor() }
}

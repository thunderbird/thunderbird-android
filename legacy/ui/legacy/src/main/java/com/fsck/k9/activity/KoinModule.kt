package com.fsck.k9.activity

import org.koin.dsl.module

val activityModule = module {
    single { MessageLoaderHelperFactory(messageViewInfoExtractorFactory = get(), htmlSettingsProvider = get()) }
}

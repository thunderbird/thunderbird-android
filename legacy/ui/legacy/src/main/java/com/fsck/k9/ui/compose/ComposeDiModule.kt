package com.fsck.k9.ui.compose

import org.koin.dsl.module

val composeDiModule = module {
    factory { IntentDataMapper() }
}

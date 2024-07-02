package com.fsck.k9.ui.identity

import org.koin.dsl.module

val identityUiModule = module {
    factory { IdentityFormatter() }
}

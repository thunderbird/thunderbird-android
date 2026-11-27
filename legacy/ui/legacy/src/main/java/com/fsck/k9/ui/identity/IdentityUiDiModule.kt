package com.fsck.k9.ui.identity

import org.koin.dsl.module

internal val identityUiDiModule = module {
    factory { IdentityFormatter() }
}

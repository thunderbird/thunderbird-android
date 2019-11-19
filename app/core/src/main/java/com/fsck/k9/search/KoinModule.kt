package com.fsck.k9.search

import org.koin.dsl.module

val searchModule = module {
    single { AccountSearchConditions() }
}

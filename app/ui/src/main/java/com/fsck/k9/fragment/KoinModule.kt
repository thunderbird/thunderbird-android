package com.fsck.k9.fragment

import org.koin.dsl.module.module

val fragmentModule = module {
    single { SortTypeToastProvider() }
}

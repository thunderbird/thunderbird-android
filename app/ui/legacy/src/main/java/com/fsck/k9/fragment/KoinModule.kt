package com.fsck.k9.fragment

import org.koin.dsl.module

val fragmentModule = module {
    single { SortTypeToastProvider() }
}

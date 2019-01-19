package com.fsck.k9.fragment

import org.koin.dsl.module.applicationContext

val fragmentModule = applicationContext {
    bean { SortTypeToastProvider() }
}

package com.fsck.k9.search

import com.fsck.k9.mailstore.*
import org.koin.dsl.module.applicationContext

val searchModule = applicationContext {
    bean { AccountSearchConditions() }
}

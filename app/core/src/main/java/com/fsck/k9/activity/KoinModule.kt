package com.fsck.k9.activity

import org.koin.dsl.module.applicationContext

val uiModule = applicationContext {
    bean { ColorChipProvider() }
}

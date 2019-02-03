package com.fsck.k9.activity

import org.koin.dsl.module.module

val activityModule = module {
    single { ColorChipProvider() }
}

package com.fsck.k9

import org.koin.core.module.Module

class K9App : CommonApp() {
    override fun provideAppModule(): Module = appModule
}

package net.thunderbird.android

import com.fsck.k9.CommonApp
import org.koin.core.module.Module

class ThunderbirdApp : CommonApp() {
    override fun provideAppModule(): Module = appModule
}

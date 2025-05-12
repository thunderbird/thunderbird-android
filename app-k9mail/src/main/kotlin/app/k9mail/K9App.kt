package app.k9mail

import net.thunderbird.app.common.BaseApplication
import org.koin.core.module.Module

class K9App : BaseApplication() {
    override fun provideAppModule(): Module = appModule
}

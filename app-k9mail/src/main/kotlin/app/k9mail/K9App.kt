package app.k9mail

import com.fsck.k9.CommonApp
import org.koin.core.module.Module

class K9App : CommonApp() {
    override fun provideAppModule(): Module = appModule
}

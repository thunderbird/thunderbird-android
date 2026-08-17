package app.k9mail

import com.fsck.k9.BuildConfig
import net.thunderbird.app.common.FeatureFlagApplication
import org.koin.core.module.Module

class K9App : FeatureFlagApplication() {
    override val appName: String = "k9"
    override val appVersion: String = BuildConfig.VERSION_NAME

    override fun provideAppModule(): Module = appModule
}

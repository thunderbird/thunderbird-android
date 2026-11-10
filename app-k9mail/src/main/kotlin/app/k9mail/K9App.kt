package app.k9mail

import com.fsck.k9.BuildConfig
import net.thunderbird.app.common.FeatureFlagApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class K9App : FeatureFlagApplication() {
    override val appName: String = "k9"
    override val appVersion: String = BuildConfig.VERSION_NAME
    override val buildType: String = BuildConfig.BUILD_TYPE

    override fun initializeKoin() {
        startKoin {
            allowOverride(false)
            androidContext(this@K9App)
            modules(appModule)
        }
    }
}

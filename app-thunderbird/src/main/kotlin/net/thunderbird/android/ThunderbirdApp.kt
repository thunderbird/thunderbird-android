package net.thunderbird.android

import app.k9mail.feature.telemetry.api.TelemetryManager
import com.fsck.k9.K9
import net.thunderbird.app.common.FeatureFlagApplication
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ThunderbirdApp : FeatureFlagApplication() {
    private val telemetryManager: TelemetryManager by inject()

    override val appName: String = "thunderbird"
    override val appVersion: String = BuildConfig.VERSION_NAME
    override val buildType: String = BuildConfig.BUILD_TYPE

    override fun initializeKoin() {
        startKoin {
            allowOverride(false)
            androidContext(this@ThunderbirdApp)
            modules(thunderbirdDiModule)
        }
    }

    override fun onCreate() {
        super.onCreate()

        initializeTelemetry()
    }

    private fun initializeTelemetry() {
        telemetryManager.init(
            uploadEnabled = K9.isTelemetryEnabled,
            releaseChannel = BuildConfig.GLEAN_RELEASE_CHANNEL,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
        )
    }
}

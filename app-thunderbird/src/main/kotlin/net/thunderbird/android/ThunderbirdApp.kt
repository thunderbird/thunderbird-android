package net.thunderbird.android

import app.k9mail.feature.telemetry.api.TelemetryManager
import com.fsck.k9.K9
import net.thunderbird.app.common.BaseApplication
import org.koin.android.ext.android.inject
import org.koin.core.module.Module

class ThunderbirdApp : BaseApplication() {
    private val telemetryManager: TelemetryManager by inject()

    override fun provideAppModule(): Module = appModule

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

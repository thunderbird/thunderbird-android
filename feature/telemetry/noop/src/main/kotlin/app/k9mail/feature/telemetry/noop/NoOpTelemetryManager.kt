package app.k9mail.feature.telemetry.noop

import app.k9mail.feature.telemetry.api.TelemetryManager

class NoOpTelemetryManager : TelemetryManager {
    override fun isTelemetryFeatureIncluded(): Boolean = false

    override fun setEnabled(enable: Boolean) = Unit

    override fun init(uploadEnabled: Boolean, releaseChannel: String?, versionCode: Int, versionName: String) = Unit
}

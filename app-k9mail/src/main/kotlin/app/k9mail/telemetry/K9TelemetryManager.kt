package app.k9mail.telemetry

import app.k9mail.feature.telemetry.api.TelemetryManager

class K9TelemetryManager : TelemetryManager {
    override fun isTelemetryFeatureIncluded(): Boolean = false
}

package app.k9mail.feature.telemetry.glean

import app.k9mail.feature.telemetry.api.TelemetryManager

class GleanTelemetryManager : TelemetryManager {
    override fun isTelemetryFeatureIncluded(): Boolean = true
}

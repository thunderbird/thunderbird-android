package app.k9mail.feature.telemetry.api

interface TelemetryManager {
    /**
     * Returns `true` if the app has a telemetry feature included.
     */
    fun isTelemetryFeatureIncluded(): Boolean
}

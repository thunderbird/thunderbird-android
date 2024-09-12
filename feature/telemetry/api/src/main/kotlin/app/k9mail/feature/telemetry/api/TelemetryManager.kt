package app.k9mail.feature.telemetry.api

interface TelemetryManager {
    /**
     * Returns `true` if the app has a telemetry feature included.
     */
    fun isTelemetryFeatureIncluded(): Boolean

    /**
     * Enable or disable telemetry.
     */
    fun setEnabled(enable: Boolean)

    /**
     * Initialize the telemetry library.
     */
    fun init(uploadEnabled: Boolean, releaseChannel: String?, versionCode: Int, versionName: String)
}

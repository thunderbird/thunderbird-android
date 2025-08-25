package app.k9mail.feature.telemetry.glean

import android.content.Context
import app.k9mail.feature.telemetry.api.TelemetryManager
import java.util.Calendar
import mozilla.components.lib.fetch.okhttp.OkHttpClient
import mozilla.components.service.glean.net.ConceptFetchHttpUploader
import mozilla.telemetry.glean.BuildInfo
import mozilla.telemetry.glean.Glean
import mozilla.telemetry.glean.config.Configuration

class GleanTelemetryManager(
    private val context: Context,
    private val okHttpClient: Lazy<okhttp3.OkHttpClient>,
) : TelemetryManager {
    override fun isTelemetryFeatureIncluded(): Boolean = true

    override fun setEnabled(enable: Boolean) {
        Glean.setCollectionEnabled(enable)
    }

    override fun init(uploadEnabled: Boolean, releaseChannel: String?, versionCode: Int, versionName: String) {
        val httpClient = lazy { OkHttpClient(okHttpClient.value, context) }

        val configuration = Configuration(
            httpClient = ConceptFetchHttpUploader(httpClient),
            channel = releaseChannel,
        )

        // We don't care for the build date (and including it would make reproducible builds harder).
        val buildDate = Calendar.getInstance().apply { clear() }

        val buildInfo = BuildInfo(
            versionCode = versionCode.toString(),
            versionName = versionName,
            buildDate = buildDate,
        )

        Glean.initialize(
            context,
            uploadEnabled,
            configuration,
            buildInfo,
        )
    }
}

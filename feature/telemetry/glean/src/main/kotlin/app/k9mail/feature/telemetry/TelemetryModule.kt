package app.k9mail.feature.telemetry

import app.k9mail.feature.telemetry.api.TelemetryManager
import app.k9mail.feature.telemetry.glean.GleanTelemetryManager
import org.koin.core.module.Module
import org.koin.dsl.module

val telemetryModule: Module = module {
    single<TelemetryManager> {
        GleanTelemetryManager(
            context = get(),
            okHttpClient = lazy { get() },
        )
    }
}

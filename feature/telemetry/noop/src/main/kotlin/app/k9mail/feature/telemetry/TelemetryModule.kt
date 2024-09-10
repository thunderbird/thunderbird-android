package app.k9mail.feature.telemetry

import app.k9mail.feature.telemetry.api.TelemetryManager
import app.k9mail.feature.telemetry.noop.NoOpTelemetryManager
import org.koin.core.module.Module
import org.koin.dsl.module

val telemetryModule: Module = module {
    single<TelemetryManager> { NoOpTelemetryManager() }
}

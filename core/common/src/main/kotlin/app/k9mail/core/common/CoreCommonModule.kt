package app.k9mail.core.common

import kotlinx.datetime.Clock
import org.koin.core.module.Module
import org.koin.dsl.module

val coreCommonModule: Module = module {
    single<Clock> { Clock.System }
}

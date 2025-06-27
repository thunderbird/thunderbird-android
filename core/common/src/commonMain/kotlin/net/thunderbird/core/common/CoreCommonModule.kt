package net.thunderbird.core.common

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.common.oauth.InMemoryOAuthConfigurationProvider
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

val coreCommonModule: Module = module {
    @OptIn(ExperimentalTime::class)
    single<Clock> { Clock.System }

    single<OAuthConfigurationProvider> {
        InMemoryOAuthConfigurationProvider(
            configurationFactory = get(),
        )
    }
}

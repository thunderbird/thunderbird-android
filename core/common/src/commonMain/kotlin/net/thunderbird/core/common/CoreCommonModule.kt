package net.thunderbird.core.common

import kotlinx.datetime.Clock
import net.thunderbird.core.common.oauth.InMemoryOAuthConfigurationProvider
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

val coreCommonModule: Module = module {
    single<Clock> { Clock.System }

    single<OAuthConfigurationProvider> {
        InMemoryOAuthConfigurationProvider(
            configurationFactory = get(),
        )
    }
}

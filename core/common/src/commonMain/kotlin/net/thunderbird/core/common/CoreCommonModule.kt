package net.thunderbird.core.common

import kotlinx.datetime.Clock
import net.thunderbird.core.common.oauth.InMemoryOAuthConfigurationProvider
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect val platformCoreCommonModule: Module

val coreCommonModule: Module = module {
    includes(platformCoreCommonModule)
    single<Clock> { Clock.System }

    single<OAuthConfigurationProvider> {
        InMemoryOAuthConfigurationProvider(
            configurationFactory = get(),
        )
    }
}

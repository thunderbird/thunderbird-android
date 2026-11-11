package net.thunderbird.core.common

import kotlin.time.Clock
import net.thunderbird.core.common.oauth.InMemoryOAuthConfigurationProvider
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val coreCommonDiModule: Module = module {
    single<Clock> { Clock.System }

    single<InMemoryOAuthConfigurationProvider>() bind OAuthConfigurationProvider::class
}

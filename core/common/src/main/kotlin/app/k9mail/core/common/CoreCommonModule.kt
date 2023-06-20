package app.k9mail.core.common

import app.k9mail.core.common.oauth.InMemoryOAuthConfigurationProvider
import app.k9mail.core.common.oauth.OAuthConfigurationProvider
import kotlinx.datetime.Clock
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

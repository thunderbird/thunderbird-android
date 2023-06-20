package app.k9mail.core.android.common.test

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import org.koin.dsl.module

internal val externalModule = module {
    single<OAuthConfigurationFactory> {
        OAuthConfigurationFactory { emptyMap() }
    }
}

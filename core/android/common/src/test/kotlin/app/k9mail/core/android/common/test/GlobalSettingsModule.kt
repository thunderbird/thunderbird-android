package app.k9mail.core.android.common.test

import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import org.koin.dsl.module

internal val externalModule = module {
    single<OAuthConfigurationFactory> {
        OAuthConfigurationFactory { emptyMap() }
    }
}

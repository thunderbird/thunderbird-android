package app.k9mail.core.android.common.test

import app.k9mail.core.common.oauth.OAuthProvider
import app.k9mail.core.common.oauth.OAuthProviderSettings
import org.koin.dsl.module

internal val externalModule = module {
    single<OAuthProviderSettings> {
        OAuthProviderSettings(
            applicationId = "test",
            clientIds = OAuthProvider.values().associateWith { "testClientId" },
            redirectUriIds = OAuthProvider.values().associateWith { "testRedirectUriId" },
        )
    }
}

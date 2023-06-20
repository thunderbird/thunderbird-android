package app.k9mail.feature.preview

import app.k9mail.core.common.oauth.OAuthProviderSettings
import app.k9mail.feature.account.setup.featureAccountSetupModule
import app.k9mail.feature.preview.config.createOAuthProviderSettings
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

val featureModule: Module = module {

    // TODO move to network module
    single<OkHttpClient> {
        OkHttpClient()
    }

    single<OAuthProviderSettings> { createOAuthProviderSettings() }

    includes(featureAccountSetupModule)
}

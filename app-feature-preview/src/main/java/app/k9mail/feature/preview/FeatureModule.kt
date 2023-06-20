package app.k9mail.feature.preview

import app.k9mail.core.common.oauth.OAuthProvider
import app.k9mail.core.common.oauth.OAuthProviderSettings
import app.k9mail.feature.account.setup.featureAccountSetupModule
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

val featureModule: Module = module {

    // TODO move to network module
    single<OkHttpClient> {
        OkHttpClient()
    }

    single<OAuthProviderSettings> {
        OAuthProviderSettings(
            applicationId = "com.fsck.k9.debug",
            clientIds = mapOf(
                OAuthProvider.AOL to BuildConfig.OAUTH_AOL_CLIENT_ID,
                OAuthProvider.GMAIL to BuildConfig.OAUTH_GMAIL_CLIENT_ID,
                OAuthProvider.MICROSOFT to BuildConfig.OAUTH_MICROSOFT_CLIENT_ID,
                OAuthProvider.YAHOO to BuildConfig.OAUTH_YAHOO_CLIENT_ID,
            ),
            redirectUriIds = mapOf(
                OAuthProvider.MICROSOFT to BuildConfig.OAUTH_MICROSOFT_REDIRECT_URI_ID,
            ),
        )
    }

    includes(featureAccountSetupModule)
}

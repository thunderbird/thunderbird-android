package app.k9mail.feature.preview

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.edit.AccountEditExternalContract
import app.k9mail.feature.account.edit.featureAccountEditModule
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import app.k9mail.feature.account.setup.featureAccountSetupModule
import app.k9mail.feature.preview.account.AccountOwnerNameProvider
import app.k9mail.feature.preview.account.InMemoryAccountStore
import app.k9mail.feature.preview.auth.AndroidKeyStoreDirectoryProvider
import app.k9mail.feature.preview.auth.AppOAuthConfigurationFactory
import app.k9mail.feature.preview.auth.DefaultTrustedSocketFactory
import app.k9mail.feature.preview.backend.RealOAuth2TokenProviderFactory
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.ssl.KeyStoreDirectoryProvider
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

val accountModule: Module = module {
    single { InMemoryAccountStore() }
        .binds(
            arrayOf(
                AccountCommonExternalContract.AccountStateLoader::class,
                AccountSetupExternalContract.AccountCreator::class,
                AccountEditExternalContract.AccountServerSettingsUpdater::class,
            ),
        )
    factory<AccountSetupExternalContract.AccountOwnerNameProvider> { AccountOwnerNameProvider() }
}

val featureModule: Module = module {
    single<OAuthConfigurationFactory> { AppOAuthConfigurationFactory() }

    factory<KeyStoreDirectoryProvider> { AndroidKeyStoreDirectoryProvider(context = get()) }
    single { LocalKeyStore(directoryProvider = get()) }
    single { TrustManagerFactory.createInstance(get()) }
    single<TrustedSocketFactory> { DefaultTrustedSocketFactory(get(), get()) }
    single<OAuth2TokenProviderFactory> { RealOAuth2TokenProviderFactory(context = get()) }

    includes(
        accountModule,
        featureAccountSetupModule,
        featureAccountEditModule,
    )
}

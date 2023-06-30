package app.k9mail.feature.preview

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import app.k9mail.feature.account.setup.featureAccountSetupModule
import app.k9mail.feature.preview.account.AccountCreator
import app.k9mail.feature.preview.account.AccountOwnerNameProvider
import app.k9mail.feature.preview.account.AccountSetupFinishedLauncher
import app.k9mail.feature.preview.auth.AndroidKeyStoreDirectoryProvider
import app.k9mail.feature.preview.auth.AppOAuthConfigurationFactory
import app.k9mail.feature.preview.auth.DefaultTrustedSocketFactory
import com.fsck.k9.mail.ssl.KeyStoreDirectoryProvider
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val accountModule: Module = module {
    factory<AccountSetupExternalContract.AccountOwnerNameProvider> { AccountOwnerNameProvider() }
    factory<AccountSetupExternalContract.AccountCreator> { AccountCreator() }
    factory<AccountSetupExternalContract.AccountSetupFinishedLauncher> {
        AccountSetupFinishedLauncher(
            context = androidContext(),
        )
    }
}

val featureModule: Module = module {
    // TODO move to network module
    single<OkHttpClient> {
        OkHttpClient()
    }

    single<OAuthConfigurationFactory> { AppOAuthConfigurationFactory() }

    factory<KeyStoreDirectoryProvider> { AndroidKeyStoreDirectoryProvider(context = get()) }
    single { LocalKeyStore(directoryProvider = get()) }
    single { TrustManagerFactory.createInstance(get()) }
    single<TrustedSocketFactory> { DefaultTrustedSocketFactory(get(), get()) }

    includes(featureAccountSetupModule, accountModule)
}

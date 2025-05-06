package com.fsck.k9

import android.content.Context
import app.k9mail.core.android.common.coreCommonAndroidModule
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.DefaultTrustedSocketFactory
import com.fsck.k9.logging.Logger
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mailstore.LocalStoreProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mainModule = module {
    single<Logger> { TimberLogger() }
    includes(coreCommonAndroidModule)
    single<CoroutineScope>(named("AppCoroutineScope")) { GlobalScope }
    single {
        Preferences(
            storagePersister = get(),
            localStoreProvider = get(),
            accountPreferenceSerializer = get(),
            accountDefaultsProvider = get(),
        )
    }
    single { get<Context>().resources }
    single { get<Context>().contentResolver }
    single { LocalStoreProvider() }
    single { Contacts() }
    single { LocalKeyStore(directoryProvider = get()) }
    single { TrustManagerFactory.createInstance(get()) }
    single { LocalKeyStoreManager(get()) }
    single<TrustedSocketFactory> { DefaultTrustedSocketFactory(get(), get()) }
    factory { EmailAddressValidator() }
    factory { ServerSettingsSerializer() }
}

package com.fsck.k9

import android.content.Context
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.setup.ServerNameSuggester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mainModule = module {
    single<CoroutineScope>(named("AppCoroutineScope")) { GlobalScope }
    single {
        Preferences(
            storagePersister = get(),
            localStoreProvider = get(),
            accountPreferenceSerializer = get()
        )
    }
    single { get<Context>().resources }
    single { get<Context>().contentResolver }
    single { LocalStoreProvider() }
    single { Contacts.getInstance(get()) }
    single { LocalKeyStore(directoryProvider = get()) }
    single { TrustManagerFactory.createInstance(get()) }
    single { LocalKeyStoreManager(get()) }
    single<TrustedSocketFactory> { DefaultTrustedSocketFactory(get(), get()) }
    single<Clock> { RealClock() }
    factory { ServerNameSuggester() }
    factory { EmailAddressValidator() }
    factory { ServerSettingsSerializer() }
}

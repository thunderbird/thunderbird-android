package com.fsck.k9

import android.content.Context
import app.k9mail.core.android.common.coreCommonAndroidModule
import com.fsck.k9.autocrypt.autocryptModule
import com.fsck.k9.controller.controllerModule
import com.fsck.k9.controller.push.controllerPushModule
import com.fsck.k9.crypto.openPgpModule
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.DefaultTrustedSocketFactory
import com.fsck.k9.helper.helperModule
import com.fsck.k9.job.jobModule
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.legacyMailStoreModule
import com.fsck.k9.message.extractors.extractorModule
import com.fsck.k9.message.html.htmlModule
import com.fsck.k9.message.quote.quoteModule
import com.fsck.k9.notification.coreNotificationModule
import com.fsck.k9.power.powerModule
import com.fsck.k9.preferences.preferencesModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import net.thunderbird.core.android.logging.loggingModule
import net.thunderbird.core.android.network.coreAndroidNetworkModule
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.feature.account.storage.legacy.featureAccountStorageLegacyModule
import org.koin.core.qualifier.named
import org.koin.dsl.module

val legacyCoreModule = module {
    includes(
        coreCommonAndroidModule,
        coreAndroidNetworkModule,
        openPgpModule,
        autocryptModule,
        legacyMailStoreModule,
        extractorModule,
        htmlModule,
        quoteModule,
        coreNotificationModule,
        controllerModule,
        controllerPushModule,
        jobModule,
        helperModule,
        preferencesModule,
        powerModule,
        loggingModule,
        featureAccountStorageLegacyModule,
    )

    single<CoroutineScope>(named("AppCoroutineScope")) { GlobalScope }
    single {
        Preferences(
            storagePersister = get(),
            localStoreProvider = get(),
            legacyAccountStorageHandler = get(),
            accountDefaultsProvider = get(),
        )
    }
    single<Storage> { get<Preferences>().storage }
    single<StorageEditor> { get<Preferences>().createStorageEditor() }
    single { get<Context>().resources }
    single { get<Context>().contentResolver }
    single { LocalStoreProvider() }
    single { Contacts() }
    single { LocalKeyStore(directoryProvider = get()) }
    single { TrustManagerFactory.createInstance(get()) }
    single { LocalKeyStoreManager(get()) }
    single<TrustedSocketFactory> { DefaultTrustedSocketFactory(get(), get()) }
    factory { EmailAddressValidator() }
}

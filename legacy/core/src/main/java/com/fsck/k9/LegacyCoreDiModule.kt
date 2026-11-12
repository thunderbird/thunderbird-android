package com.fsck.k9

import android.content.Context
import net.thunderbird.core.android.common.coreAndroidCommonDiModule
import com.fsck.k9.autocrypt.autocryptDiModule
import com.fsck.k9.controller.controllerDiModule
import com.fsck.k9.controller.push.controllerPushDiModule
import com.fsck.k9.crypto.openPgpDiModule
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.DefaultTrustedSocketFactory
import com.fsck.k9.helper.helperDiModule
import com.fsck.k9.job.jobDiModule
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.legacyMailStoreDiModule
import com.fsck.k9.message.extractors.extractorDiModule
import com.fsck.k9.message.html.htmlDiModule
import com.fsck.k9.message.quote.quoteDiModule
import com.fsck.k9.notification.coreNotificationDiModule
import com.fsck.k9.power.powerDiModule
import com.fsck.k9.preferences.preferencesDiModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import net.thunderbird.core.android.network.coreAndroidNetworkDiModule
import net.thunderbird.core.preference.storage.DefaultStorageProvider
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.storage.legacy.featureAccountStorageLegacyDiModule
import org.koin.core.qualifier.named
import org.koin.dsl.module

val legacyCoreDiModule = module {
    includes(
        coreAndroidCommonDiModule,
        coreAndroidNetworkDiModule,
        openPgpDiModule,
        autocryptDiModule,
        legacyMailStoreDiModule,
        extractorDiModule,
        htmlDiModule,
        quoteDiModule,
        coreNotificationDiModule,
        controllerDiModule,
        controllerPushDiModule,
        jobDiModule,
        helperDiModule,
        preferencesDiModule,
        powerDiModule,
        featureAccountStorageLegacyDiModule,
    )

    single<CoroutineScope>(named("AppCoroutineScope")) { GlobalScope }
    single<StorageProvider> {
        DefaultStorageProvider(
            storagePersister = get(),
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
}

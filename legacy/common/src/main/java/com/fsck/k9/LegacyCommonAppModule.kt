package com.fsck.k9

import app.k9mail.feature.widget.message.list.messageListWidgetModule
import app.k9mail.feature.widget.unread.UnreadWidgetUpdateListener
import app.k9mail.feature.widget.unread.unreadWidgetModule
import com.fsck.k9.account.newAccountModule
import com.fsck.k9.backends.backendsModule
import com.fsck.k9.controller.ControllerExtension
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.crypto.openpgp.OpenPgpEncryptionExtractor
import com.fsck.k9.notification.notificationModule
import com.fsck.k9.preferences.K9StoragePersister
import com.fsck.k9.preferences.StoragePersister
import com.fsck.k9.resources.resourcesModule
import com.fsck.k9.storage.storageModule
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.InMemoryFeatureFlagProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

val legacyCommonAppModule = module {
    single {
        MessagingListenerProvider(
            listOf(
                get<UnreadWidgetUpdateListener>(),
            ),
        )
    }
    single(named("controllerExtensions")) { emptyList<ControllerExtension>() }
    single<EncryptionExtractor> { OpenPgpEncryptionExtractor.newInstance() }
    single<StoragePersister> { K9StoragePersister(get()) }
    single<FeatureFlagProvider> {
        InMemoryFeatureFlagProvider(
            featureFlagFactory = get(),
        )
    }
}

val legacyCommonAppModules = listOf(
    legacyCommonAppModule,
    messageListWidgetModule,
    unreadWidgetModule,
    notificationModule,
    resourcesModule,
    backendsModule,
    storageModule,
    newAccountModule,
)

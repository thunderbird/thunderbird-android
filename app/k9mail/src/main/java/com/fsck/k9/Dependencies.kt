package com.fsck.k9

import app.k9mail.ui.widget.list.messageListWidgetModule
import com.fsck.k9.auth.createOAuthConfigurationProvider
import com.fsck.k9.backends.backendsModule
import com.fsck.k9.controller.ControllerExtension
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.crypto.openpgp.OpenPgpEncryptionExtractor
import com.fsck.k9.notification.notificationModule
import com.fsck.k9.preferences.K9StoragePersister
import com.fsck.k9.preferences.StoragePersister
import com.fsck.k9.resources.resourcesModule
import com.fsck.k9.storage.storageModule
import com.fsck.k9.widget.list.messageListWidgetConfigModule
import com.fsck.k9.widget.unread.UnreadWidgetUpdateListener
import com.fsck.k9.widget.unread.unreadWidgetModule
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val mainAppModule = module {
    single { App.appConfig }
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
    single { createOAuthConfigurationProvider() }
}

val appModules = listOf(
    mainAppModule,
    messageListWidgetConfigModule,
    messageListWidgetModule,
    unreadWidgetModule,
    notificationModule,
    resourcesModule,
    backendsModule,
    storageModule,
)

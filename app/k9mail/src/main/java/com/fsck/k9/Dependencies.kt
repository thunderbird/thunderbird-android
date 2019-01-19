package com.fsck.k9

import com.fsck.k9.backends.backendsModule
import com.fsck.k9.controller.ControllerExtension
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.crypto.openpgp.OpenPgpEncryptionExtractor
import com.fsck.k9.external.BroadcastSenderListener
import com.fsck.k9.external.externalModule
import com.fsck.k9.notification.notificationModule
import com.fsck.k9.preferences.K9StoragePersister
import com.fsck.k9.preferences.StoragePersister
import com.fsck.k9.resources.resourcesModule
import com.fsck.k9.storage.storageModule
import com.fsck.k9.widget.list.MessageListWidgetUpdateListener
import com.fsck.k9.widget.list.messageListWidgetModule
import com.fsck.k9.widget.unread.UnreadWidgetUpdateListener
import com.fsck.k9.widget.unread.unreadWidgetModule
import org.koin.dsl.module.applicationContext

private val mainAppModule = applicationContext {
    bean { App.appConfig }
    bean { MessagingListenerProvider(
            listOf(
                    get<UnreadWidgetUpdateListener>(),
                    get<MessageListWidgetUpdateListener>(),
                    get<BroadcastSenderListener>()
            ))
    }
    bean("controllerExtensions") { emptyList<ControllerExtension>() }
    bean { OpenPgpEncryptionExtractor.newInstance() as EncryptionExtractor }
    bean { K9StoragePersister(get()) as StoragePersister }
}

val appModules = listOf(
        mainAppModule,
        externalModule,
        messageListWidgetModule,
        unreadWidgetModule,
        notificationModule,
        resourcesModule,
        backendsModule,
        storageModule
)

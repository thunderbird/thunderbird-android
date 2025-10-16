package com.fsck.k9.controller

import android.content.Context
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageCountsProvider
import app.k9mail.legacy.message.controller.MessagingControllerRegistry
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.SaveMessageDataCreator
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.notification.NotificationStrategy
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.notification.api.NotificationManager
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

val controllerModule = module {
    single {
        MessagingController(
            get<Context>(),
            get<NotificationController>(),
            get<NotificationStrategy>(),
            get<LocalStoreProvider>(),
            get<BackendManager>(),
            get<Preferences>(),
            get<MessageStoreManager>(),
            get<SaveMessageDataCreator>(),
            get<SpecialLocalFoldersCreator>(),
            get<LocalDeleteOperationDecider>(),
            get(named("controllerExtensions")),
            get<FeatureFlagProvider>(),
            get<Logger>(named("syncDebug")),
            get<NotificationManager>(),
            get<OutboxFolderManager>(),
        )
    } binds arrayOf(MessagingControllerRegistry::class)

    single {
        MessagingControllerWrapper(
            messagingController = get(),
            accountManager = get(),
        )
    }

    single<MessagingControllerRegistry> { get<MessagingController>() }

    single<MessageCountsProvider> {
        DefaultMessageCountsProvider(
            accountManager = get(),
            messageStoreManager = get(),
            messagingControllerRegistry = get(),
            outboxFolderManager = get(),
        )
    }

    single { LocalDeleteOperationDecider() }
}

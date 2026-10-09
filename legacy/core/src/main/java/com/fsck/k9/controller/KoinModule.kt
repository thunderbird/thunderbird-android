package com.fsck.k9.controller

import android.content.Context
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.domain.GetFolderIdsForTypeUseCase
import app.k9mail.legacy.mailstore.domain.SetPushForFolderUseCase
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
import net.thunderbird.feature.mail.message.list.LocalDeleteOperationDecider
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider
import net.thunderbird.feature.notification.api.NotificationManager
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

val controllerModule = module {
    single {
        MessagingController(
            logger = get<Logger>(),
            context = get<Context>(),
            notificationController = get<NotificationController>(),
            notificationStrategy = get<NotificationStrategy>(),
            localStoreProvider = get<LocalStoreProvider>(),
            backendManager = get<BackendManager>(),
            preferences = get<Preferences>(),
            messageStoreManager = get<MessageStoreManager>(),
            saveMessageDataCreator = get<SaveMessageDataCreator>(),
            localDeleteOperationDecider = get<LocalDeleteOperationDecider>(),
            localMessageUidPrefixProvider = get<LocalMessageUidPrefixProvider>(),
            controllerExtensions = get(named("controllerExtensions")),
            featureFlagProvider = get<FeatureFlagProvider>(),
            syncDebugLogger = get<Logger>(named("syncDebug")),
            notificationManager = get<NotificationManager>(),
            outboxFolderManager = get<OutboxFolderManager>(),
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

    single<GetFolderIdsForTypeUseCase> {
        GetFolderIdsForTypeUseCase(
            messageStoreManager = get(),
        )
    }

    single<SetPushForFolderUseCase> {
        SetPushForFolderUseCase(
            messageStoreManager = get(),
        )
    }
}

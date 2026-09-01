package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.DefaultFolderQueryRepository
import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.folder.DefaultFolderDetailsRepository
import app.k9mail.legacy.mailstore.folder.DefaultRemoteFolderDetailsRepository
import app.k9mail.legacy.mailstore.folder.DefaultRemoteFolderQueryRepository
import app.k9mail.legacy.mailstore.folder.push.DefaultPushFolderTrackingRepository
import app.k9mail.legacy.mailstore.folder.push.DefaultPushFoldersQueryRepository
import com.fsck.k9.mailstore.folder.DefaultOutboxFolderManager
import com.fsck.k9.message.extractors.AttachmentCounter
import com.fsck.k9.message.extractors.MessageFulltextCreator
import com.fsck.k9.message.extractors.MessagePreviewCreator
import kotlin.time.ExperimentalTime
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.common.cache.TimeLimitedCache
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.data.repository.FolderDetailsRepository
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository
import net.thunderbird.feature.mail.folder.api.data.repository.PushFolderTrackingRepository
import net.thunderbird.feature.mail.folder.api.data.repository.PushFoldersQueryRepository
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderDetailsRepository
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderQueryRepository
import org.koin.dsl.module

val mailStoreModule = module {
    single<PushFolderTrackingRepository> {
        DefaultPushFolderTrackingRepository(logger = get(), messageStoreManager = get())
    }
    single<PushFoldersQueryRepository> {
        DefaultPushFoldersQueryRepository(
            logger = get(),
            messageStoreManager = get(),
            remoteFolderDetailsRepository = get(),
        )
    }
    single<PushFoldersQueryRepository> {
        DefaultPushFoldersQueryRepository(
            logger = get(),
            messageStoreManager = get(),
            remoteFolderDetailsRepository = get(),
        )
    }
    single<FolderDetailsRepository> {
        DefaultFolderDetailsRepository(
            logger = get(),
            accountManager = get(),
            outboxFolderManager = get(),
            messageStoreManager = get(),
        )
    }
    single<RemoteFolderDetailsRepository> {
        DefaultRemoteFolderDetailsRepository(
            logger = get(),
            messageStoreManager = get(),
        )
    }
    single<RemoteFolderQueryRepository> {
        DefaultRemoteFolderQueryRepository(logger = get(), messageStoreManager = get())
    }
    single<FolderQueryRepository> {
        DefaultFolderQueryRepository(
            logger = get(),
            accountManager = get(),
            messageStoreManager = get(),
            outboxFolderManager = get(),
        )
    }
    single { MessageViewInfoExtractorFactory(get(), get(), get()) }
    single<StorageFilesProviderFactory> { AndroidStorageFilesProviderFactory(context = get()) }
    single { SpecialFolderSelectionStrategy() }
    single<BackendStorageFactory> {
        K9BackendStorageFactory(
            preferences = get(),
            accountManager = get(),
            folderQueryRepository = get(),
            messageStoreManager = get(),
            specialFolderUpdaterFactory = get(),
            saveMessageDataCreator = get(),
        )
    }
    factory { SpecialLocalFoldersCreator(preferences = get(), localStoreProvider = get(), outboxFolderManager = get()) }
    single { MessageStoreManager(accountManager = get(), messageStoreFactory = get()) }
    single { MessageRepository(messageStoreManager = get()) }
    factory { MessagePreviewCreator.newInstance() }
    factory { MessageFulltextCreator.newInstance() }
    factory { AttachmentCounter.newInstance() }
    factory {
        SaveMessageDataCreator(
            encryptionExtractor = get(),
            messagePreviewCreator = get(),
            messageFulltextCreator = get(),
            attachmentCounter = get(),
        )
    }
    single<MessageListRepository> { DefaultMessageListRepository(messageStoreManager = get()) }
    single<OutboxFolderManager> {
        DefaultOutboxFolderManager(
            logger = get(),
            accountManager = get(),
            localStoreProvider = get(),
            outboxFolderIdCache = TimeLimitedCache(),
        )
    }
}

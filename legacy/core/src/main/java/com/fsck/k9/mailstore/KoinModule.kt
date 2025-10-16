package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.FolderRepository
import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.mailstore.folder.DefaultOutboxFolderManager
import com.fsck.k9.message.extractors.AttachmentCounter
import com.fsck.k9.message.extractors.MessageFulltextCreator
import com.fsck.k9.message.extractors.MessagePreviewCreator
import kotlin.time.ExperimentalTime
import net.thunderbird.core.common.cache.TimeLimitedCache
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import org.koin.dsl.module

val mailStoreModule = module {
    single {
        FolderRepository(
            messageStoreManager = get(),
            outboxFolderManager = get(),
        )
    }
    single { MessageViewInfoExtractorFactory(get(), get(), get()) }
    single<StorageFilesProviderFactory> { AndroidStorageFilesProviderFactory(context = get()) }
    single { SpecialFolderSelectionStrategy() }
    single<LegacyAccountDtoBackendStorageFactory> {
        K9BackendStorageFactory(
            preferences = get(),
            folderRepository = get(),
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
            outboxFolderIdCache = @OptIn(ExperimentalTime::class)TimeLimitedCache(),
        )
    }
}

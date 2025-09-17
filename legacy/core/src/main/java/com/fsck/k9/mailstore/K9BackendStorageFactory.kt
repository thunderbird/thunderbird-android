package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.FolderRepository
import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.Preferences
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

class K9BackendStorageFactory(
    private val preferences: Preferences,
    private val folderRepository: FolderRepository,
    private val messageStoreManager: MessageStoreManager,
    private val specialFolderUpdaterFactory: SpecialFolderUpdater.Factory<LegacyAccountDto>,
    private val saveMessageDataCreator: SaveMessageDataCreator,
) : LegacyAccountDtoBackendStorageFactory {
    override fun createBackendStorage(account: LegacyAccountDto): K9BackendStorage {
        val messageStore = messageStoreManager.getMessageStore(account)
        val folderSettingsProvider = FolderSettingsProvider(preferences, account)
        val specialFolderUpdater = specialFolderUpdaterFactory.create(account)
        val specialFolderListener = SpecialFolderBackendFoldersRefreshListener(specialFolderUpdater)
        val autoExpandFolderListener = AutoExpandFolderBackendFoldersRefreshListener(
            preferences,
            account,
            folderRepository,
        )
        val listeners = listOf(specialFolderListener, autoExpandFolderListener)
        return K9BackendStorage(
            messageStore = messageStore,
            folderSettingsProvider = folderSettingsProvider,
            saveMessageDataCreator = saveMessageDataCreator,
            listeners = listeners,
        )
    }
}

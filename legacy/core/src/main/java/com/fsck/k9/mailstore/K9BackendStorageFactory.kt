package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.FolderRepository
import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.Preferences
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

class K9BackendStorageFactory(
    private val preferences: Preferences,
    private val accountManager: LegacyAccountManager,
    private val folderRepository: FolderRepository,
    private val messageStoreManager: MessageStoreManager,
    private val specialFolderUpdaterFactory: SpecialFolderUpdater.Factory,
    private val saveMessageDataCreator: SaveMessageDataCreator,
) : BackendStorageFactory {
    override fun createBackendStorage(accountId: AccountId): K9BackendStorage {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val folderSettingsProvider = FolderSettingsProvider(preferences, accountManager, accountId)
        val specialFolderUpdater = specialFolderUpdaterFactory.create(accountId)
        val specialFolderListener = SpecialFolderBackendFoldersRefreshListener(specialFolderUpdater)
        val autoExpandFolderListener = AutoExpandFolderBackendFoldersRefreshListener(
            accountManager,
            accountId,
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

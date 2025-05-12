package com.fsck.k9.mailstore

import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.mailstore.FolderRepository
import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.Preferences
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.mail.folder.api.SpecialFolderUpdater

class K9BackendStorageFactory(
    private val preferences: Preferences,
    private val folderRepository: FolderRepository,
    private val messageStoreManager: MessageStoreManager,
    private val specialFolderUpdaterFactory: SpecialFolderUpdater.Factory<LegacyAccount>,
    private val saveMessageDataCreator: SaveMessageDataCreator,
) : BackendStorageFactory<LegacyAccount> {
    override fun createBackendStorage(account: LegacyAccount): K9BackendStorage {
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

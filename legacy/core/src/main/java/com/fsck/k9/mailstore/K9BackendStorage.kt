package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.CreateFolderInfo
import app.k9mail.legacy.mailstore.MessageStore
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.Message
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.message.domain.MessageLifecycleRepository
import net.thunderbird.feature.mail.message.domain.MessageQueryRepository
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import com.fsck.k9.mail.FolderType as RemoteFolderType

class K9BackendStorage(
    private val logger: Logger,
    private val messageStore: MessageStore,
    private val folderSettingsProvider: FolderSettingsProvider,
    private val listeners: List<BackendFoldersRefreshListener>,
    private val messageQueryRepository: MessageQueryRepository,
    private val messageLifecycleRepository: MessageLifecycleRepository,
    private val folderIdLegacyEntityIdFactory: LegacyEntityIdFactory<FolderId>,
    private val messageDataMapper: MessageDataMapper<Message>,
) : BackendStorage {
    override fun getFolder(folderServerId: String): BackendFolder = K9BackendFolder(
        logger = logger,
        messageStore = messageStore,
        folderServerId = folderServerId,
        messageQueryRepository = messageQueryRepository,
        messageLifecycleRepository = messageLifecycleRepository,
        folderIdLegacyEntityIdFactory = folderIdLegacyEntityIdFactory,
        mapper = messageDataMapper,
    )

    override fun getFolderServerIds(): List<String> {
        return messageStore.getFolders(excludeLocalOnly = true) { folder -> folder.serverIdOrThrow() }
    }

    override fun createFolderUpdater(): BackendFolderUpdater {
        return K9BackendFolderUpdater()
    }

    override fun getExtraString(name: String): String? {
        return messageStore.getExtraString(name)
    }

    override fun setExtraString(name: String, value: String) {
        messageStore.setExtraString(name, value)
    }

    override fun getExtraNumber(name: String): Long? {
        return messageStore.getExtraNumber(name)
    }

    override fun setExtraNumber(name: String, value: Long) {
        messageStore.setExtraNumber(name, value)
    }

    private inner class K9BackendFolderUpdater : BackendFolderUpdater {
        init {
            listeners.forEach { it.onBeforeFolderListRefresh() }
        }

        override fun createFolders(folders: List<FolderInfo>): Set<Long> {
            if (folders.isEmpty()) return emptySet()

            val createFolderInfo = folders.map { folderInfo ->
                CreateFolderInfo(
                    serverId = folderInfo.serverId,
                    name = folderInfo.name,
                    type = folderInfo.type,
                    settings = folderSettingsProvider.getFolderSettings(folderInfo.serverId),
                )
            }
            return messageStore.createFolders(createFolderInfo)
        }

        override fun deleteFolders(folderServerIds: List<String>) {
            if (folderServerIds.isNotEmpty()) {
                messageStore.deleteFolders(folderServerIds)
            }
        }

        override fun changeFolder(folderServerId: String, name: String, type: RemoteFolderType) {
            messageStore.changeFolder(folderServerId, name, type)
        }

        override fun close() {
            listeners.forEach { runBlocking { it.onAfterFolderListRefresh() } }
        }
    }
}

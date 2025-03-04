package app.k9mail.backend.demo

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendFolder.MoreMessages
import com.fsck.k9.backend.api.BackendPusher
import com.fsck.k9.backend.api.BackendPusherCallback
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class DemoBackend(
    private val backendStorage: BackendStorage,
) : Backend {
    private val demoFolders by lazy { readDemoFolders() }

    override val supportsFlags: Boolean = true
    override val supportsExpunge: Boolean = false
    override val supportsMove: Boolean = true
    override val supportsCopy: Boolean = true
    override val supportsUpload: Boolean = true
    override val supportsTrashFolder: Boolean = true
    override val supportsSearchByDate: Boolean = false
    override val supportsFolderSubscriptions: Boolean = false
    override val isPushCapable: Boolean = false

    override fun refreshFolderList() {
        val localFolderServerIds = backendStorage.getFolderServerIds().toSet()

        backendStorage.updateFolders {
            val remoteFolderServerIds = demoFolders.keys
            val foldersServerIdsToCreate = remoteFolderServerIds - localFolderServerIds
            val foldersToCreate = foldersServerIdsToCreate.mapNotNull { folderServerId ->
                demoFolders[folderServerId]?.let { folderData ->
                    FolderInfo(folderServerId, folderData.name, folderData.type)
                }
            }
            createFolders(foldersToCreate)

            val folderServerIdsToRemove = (localFolderServerIds - remoteFolderServerIds).toList()
            deleteFolders(folderServerIdsToRemove)
        }
    }

    override fun sync(folderServerId: String, syncConfig: SyncConfig, listener: SyncListener) {
        listener.syncStarted(folderServerId)

        val folderData = demoFolders[folderServerId]
        if (folderData == null) {
            listener.syncFailed(folderServerId, "Folder $folderServerId doesn't exist", null)
            return
        }

        val backendFolder = backendStorage.getFolder(folderServerId)

        val localMessageServerIds = backendFolder.getMessageServerIds()
        if (localMessageServerIds.isNotEmpty()) {
            listener.syncFinished(folderServerId)
            return
        }

        for (messageServerId in folderData.messageServerIds) {
            val message = loadMessage(folderServerId, messageServerId)
            backendFolder.saveMessage(message, MessageDownloadState.FULL)
            listener.syncNewMessage(folderServerId, messageServerId, isOldMessage = false)
        }

        backendFolder.setMoreMessages(MoreMessages.FALSE)

        listener.syncFinished(folderServerId)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) = Unit

    override fun markAllAsRead(folderServerId: String) = Unit

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) = Unit

    override fun deleteAllMessages(folderServerId: String) = Unit

    override fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String> {
        // We do just enough to simulate a successful operation on the server.
        return messageServerIds.associateWith { createNewServerId() }
    }

    override fun moveMessagesAndMarkAsRead(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String> {
        // We do just enough to simulate a successful operation on the server.
        return messageServerIds.associateWith { createNewServerId() }
    }

    override fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String> {
        // We do just enough to simulate a successful operation on the server.
        return messageServerIds.associateWith { createNewServerId() }
    }

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<String> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun uploadMessage(folderServerId: String, message: Message): String {
        return createNewServerId()
    }

    override fun sendMessage(message: Message) {
        val inboxServerId = demoFolders.filterValues { it.type == FolderType.INBOX }.keys.first()
        val backendFolder = backendStorage.getFolder(inboxServerId)

        val newMessage = message.copy(uid = createNewServerId())
        backendFolder.saveMessage(newMessage, MessageDownloadState.FULL)
    }

    override fun createPusher(callback: BackendPusherCallback): BackendPusher {
        throw UnsupportedOperationException("not implemented")
    }

    private fun createNewServerId() = UUID.randomUUID().toString()

    private fun Message.copy(uid: String): MimeMessage {
        val outputStream = ByteArrayOutputStream()
        writeTo(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        return MimeMessage.parseMimeMessage(inputStream, false).apply {
            this.uid = uid
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    private fun readDemoFolders(): DemoFolders {
        return getResourceAsStream("/contents.json").use { inputStream ->
            Json.decodeFromStream<DemoFolders>(inputStream)
        }
    }

    private fun loadMessage(folderServerId: String, messageServerId: String): Message {
        return getResourceAsStream("/$folderServerId/$messageServerId.eml").use { inputStream ->
            MimeMessage.parseMimeMessage(inputStream, false).apply {
                uid = messageServerId
            }
        }
    }

    private fun getResourceAsStream(name: String): InputStream {
        return DemoBackend::class.java.getResourceAsStream(name) ?: error("Resource '$name' not found")
    }
}

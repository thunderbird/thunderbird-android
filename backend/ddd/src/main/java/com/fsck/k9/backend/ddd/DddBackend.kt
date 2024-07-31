package com.fsck.k9.backend.ddd

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendFolder
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
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import okio.buffer
import okio.source

class DddBackend(
    context: Context,
    accountName: String,
    backendStorage: BackendStorage,
) : Backend {
    private val messageStoreInfo by lazy { readMessageStoreInfo() }
    private val RESOLVER_COLUMNS = arrayOf("data")
    private val context = context
    private val backendStorage = backendStorage

    companion object {
        val CONTENT_URL: Uri = Uri.parse("content://net.discdd.provider.datastoreprovider/mails")
    }
    override val supportsFlags = false
    override val supportsExpunge = false
    override val supportsMove = false
    override val supportsCopy = false
    override val supportsUpload = false
    override val supportsTrashFolder = false
    override val supportsSearchByDate = false
    override val supportsFolderSubscriptions = false
    override val isPushCapable = false

    override fun refreshFolderList() {
        val localFolderServerIds = backendStorage.getFolderServerIds().toSet()

        backendStorage.updateFolders {
            val remoteFolderServerIds = messageStoreInfo.keys
            val foldersServerIdsToCreate = remoteFolderServerIds - localFolderServerIds
            val foldersToCreate = foldersServerIdsToCreate.mapNotNull { folderServerId ->
                messageStoreInfo[folderServerId]?.let { folderData ->
                    FolderInfo(folderServerId, folderData.name, folderData.type)
                }
            }
            createFolders(foldersToCreate)

            val folderServerIdsToRemove = (localFolderServerIds - remoteFolderServerIds).toList()
            deleteFolders(folderServerIdsToRemove)
        }
    }
    @OptIn(ExperimentalStdlibApi::class)
    private fun readMessageStoreInfo(): MessageStoreInfo {
        val initialFolders: MessageStoreInfo = try {
            getResourceAsStream("/contents_ddd.json").source().buffer().use { bufferedSource ->
                val moshi = Moshi.Builder().build()
                val adapter = moshi.adapter<MessageStoreInfo>()
                adapter.fromJson(bufferedSource)
            } ?: error("Couldn't read message store info")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }

        val aduIdList = getAllPendingMailIds()
        val inboxFolderData = FolderData(
            name = "inbox",
            type = FolderType.INBOX,
            messageServerIds = aduIdList
        )

        val combinedFolders = initialFolders.toMutableMap()
        combinedFolders["inbox"] = inboxFolderData

        return combinedFolders
    }

    private fun getAllPendingMailIds(): List<String> {
        try {
            val resolver = context.contentResolver
            val cursor = resolver.query(CONTENT_URL, RESOLVER_COLUMNS, "aduIds", null, null)

            cursor ?: throw NullPointerException("Cursor is null")

            val aduIds = mutableListOf<String>()
            if (cursor.moveToFirst()) {
                val idsString = cursor.getString(cursor.getColumnIndexOrThrow(RESOLVER_COLUMNS[0]))
                if (idsString.isNotEmpty()) {
                    val ids = idsString.split(",")
                    aduIds.addAll(ids)
                }
            }

            cursor.close()
            return aduIds
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            return emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun getResourceAsStream(name: String): InputStream {
        return DddBackend::class.java.getResourceAsStream(name) ?: error("Resource '$name' not found")
    }

    @Throws(NullPointerException::class)
    private fun loadMessage(folderServerId: String, messageServerId: String): Message {
            val resolver = context.contentResolver
            val cursor = resolver.query(CONTENT_URL, RESOLVER_COLUMNS, "aduData", arrayOf(messageServerId), null)

            cursor ?: throw NullPointerException("Cursor is null")
            var messageBytes: ByteArray? = null

            if (cursor.moveToFirst()) {
                messageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(RESOLVER_COLUMNS[0]))
            }

            cursor.close()

            if (messageBytes == null) {
                throw NullPointerException("Message bytes are null")
            }

            val inputStream = messageBytes.inputStream()
            val mimeMessage = MimeMessage.parseMimeMessage(inputStream, false).apply { uid = messageServerId }

            return mimeMessage
    }

    override fun sync(folderServerId: String, syncConfig: SyncConfig, listener: SyncListener) {
        listener.syncStarted(folderServerId)

        val folderData = messageStoreInfo["inbox"]
        if (folderData == null) {
            listener.syncFailed(folderServerId, "Folder $folderServerId doesn't exist", null)
            return
        }

        val backendFolder = backendStorage.getFolder(folderServerId)

        try {
            readMessageStoreInfo()
            for (messageServerId in folderData.messageServerIds) {
                val message = loadMessage(folderServerId, messageServerId)
                backendFolder.saveMessage(message, MessageDownloadState.FULL)
                listener.syncNewMessage(folderServerId, messageServerId, isOldMessage = false)
            }

            backendFolder.setMoreMessages(BackendFolder.MoreMessages.FALSE)

            listener.syncFinished(folderServerId)
        } catch (e: Exception) {
            e.printStackTrace()
            listener.syncFailed(folderServerId, "Unable to complete Inbox folder sync from the bundle client", e)
        }
    }


    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
//        commandDownloadMessage.downloadCompleteMessage(folderServerId, messageServerId)
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
//        commandSetFlag.setFlag(folderServerId, messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
//        commandSetFlag.setFlag(folderServerId, messageServerIds, Flag.DELETED, true)
    }

    override fun deleteAllMessages(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    private fun createNewServerId() = UUID.randomUUID().toString()

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
        throw UnsupportedOperationException("not supported")
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        throw UnsupportedOperationException("not supported")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        return null
    }

    override fun uploadMessage(folderServerId: String, message: Message): String? {
        return createNewServerId()
    }

    override fun sendMessage(message: Message) {
//        smtpTransport.sendMessage(message)
        if (message.size > 0) {
            return
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        message.writeTo(byteArrayOutputStream)

        val values = ContentValues().apply {
            put(RESOLVER_COLUMNS[0], byteArrayOutputStream.toByteArray())
        }

        try {
            val resolver = context.contentResolver
            val uri = resolver.insert(CONTENT_URL, values)
            if (uri == null) {
                throw Exception("Message not inserted")
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun createPusher(callback: BackendPusherCallback): BackendPusher {
        throw UnsupportedOperationException("not implemented")
    }
}

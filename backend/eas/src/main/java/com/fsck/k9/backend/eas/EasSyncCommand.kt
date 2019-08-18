package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.*
import com.fsck.k9.mail.internet.MimeMessage
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.max
import kotlin.math.min

const val EXTRA_SYNC_KEY = "EXTRA_SYNC_KEY"

class EasSyncCommand(private val client: EasClient,
                     private val provisionManager: EasProvisionManager,
                     private val backendStorage: BackendStorage) {

    fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        val backendFolder = backendStorage.getFolder(folder)

        var syncKey = backendFolder.getFolderExtraString(EXTRA_SYNC_KEY) ?: "0"

        var messagesLoaded = 0
        val maxMessageLoad = syncConfig.defaultVisibleLimit

        provisionManager.ensureProvisioned {
            if (syncKey == "0") {
                val syncResponse = client.sync(Sync(SyncCollections(SyncCollection(
                        "Email",
                        syncKey,
                        folder))))

                syncKey = syncResponse!!.collections!!.collection!!.syncKey!!
            }
            while (maxMessageLoad > messagesLoaded) {
                val syncResponse = client.sync(Sync(SyncCollections(SyncCollection("Email",
                        syncKey,
                        folder,
                        1,
                        options = SyncOptions(
                                mimeSupport = 2,
                                bodyPreference = SyncBodyPreference(4, syncConfig.maximumAutoDownloadMessageSize)
                        ),
                        getChanges = 1,
                        windowSize = min(30, maxMessageLoad - messagesLoaded)))))

                val collection = syncResponse!!.collections!!.collection!!

                val newSyncKey = collection.syncKey!!
                backendFolder.setFolderExtraString(EXTRA_SYNC_KEY, newSyncKey)
                val commands = collection.commands
                if (commands != null) {
                    if (commands.add?.isNotEmpty() == true) {
                        for (item in commands.add) {
                            val message = item.getMessage(EasFolder(folder))

                            if (item.isTruncated()) {
                                backendFolder.savePartialMessage(message)
                            } else {
                                backendFolder.saveCompleteMessage(message)
                            }

                            listener.syncNewMessage(folder, item.serverId, false)
                        }
                        messagesLoaded += commands.add.size
                    }

                    if (commands.delete?.isNotEmpty() == true) {
                        backendFolder.destroyMessages(commands.delete.map { it.serverId })

                        for (item in commands.delete) {
                            listener.syncRemovedMessage(folder, item.serverId)
                        }
                    }

                    if (commands.change?.isNotEmpty() == true) {
                        for (item in commands.change) {
                            item.data?.emailRead?.let {
                                backendFolder.setMessageFlag(item.serverId, Flag.SEEN, it == 1)

                                listener.syncFlagChanged(folder, item.serverId)
                            }
                        }
                    }
                }

                if (collection.moreAvailable != true) {
                    break
                }
            }
        }
    }

    fun delete(folderServerId: String, messageServerIds: List<String>) {
        executeCommands(folderServerId, SyncCommands(
                delete = messageServerIds.map { SyncItem(it) }
        ))
    }

    fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        if (flag == Flag.SEEN) {
            executeCommands(folderServerId, SyncCommands(
                    change = messageServerIds.map {
                        SyncItem(it, SyncData(emailRead = if (newState) 1 else 0))
                    }
            ))
        }
    }

    private fun executeCommands(folderServerId: String, syncCommands: SyncCommands) {
        val backendFolder = backendStorage.getFolder(folderServerId)
        val syncKey = backendFolder.getFolderExtraString(EXTRA_SYNC_KEY) ?: "0"

        provisionManager.ensureProvisioned {
            val syncResponse = client.sync(Sync(
                    SyncCollections(
                            SyncCollection(
                                    "Email",
                                    syncKey,
                                    folderServerId,
                                    1,
                                    commands = syncCommands
                            )
                    )
            ))

            println(syncResponse)

            val newSyncKey = syncResponse!!.collections!!.collection!!.syncKey!!
            backendFolder.setFolderExtraString(EXTRA_SYNC_KEY, newSyncKey)
        }
    }
}

fun SyncItem.isTruncated() = data!!.body!!.truncated == 1

fun SyncItem.getMessage(folder: EasFolder) = data!!.let {
    EasMessage(folder).apply {
        parse(it.body!!.data!!.byteInputStream())
        messageId = serverId
        uid = serverId
        if (it.emailRead == 1) {
            setFlag(Flag.SEEN, true)
        }
    }
}

class EasFolder(@JvmField val serverId: String) : Folder<EasMessage>() {
    override fun open(mode: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isOpen(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMode(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun create(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exists(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMessageCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnreadMessageCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getFlaggedMessageCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMessage(uid: String?): EasMessage {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMessages(start: Int, end: Int, earliestDate: Date?, listener: MessageRetrievalListener<EasMessage>?): MutableList<EasMessage> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun areMoreMessagesAvailable(indexOfOldestMessage: Int, earliestDate: Date?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun appendMessages(messages: MutableList<out Message>?): MutableMap<String, String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setFlags(messages: MutableList<out Message>?, flags: MutableSet<Flag>?, value: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setFlags(flags: MutableSet<Flag>?, value: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUidFromMessageId(messageId: String?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetch(messages: MutableList<EasMessage>?, fp: FetchProfile?, listener: MessageRetrievalListener<EasMessage>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getServerId() = serverId

    override fun getName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class EasMessage(folder: EasFolder) : MimeMessage() {
    init {
        mFolder = folder
    }
}

package com.fsck.k9.backend.eas

import java.io.InputStream
import java.io.OutputStream

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.backend.eas.dto.*
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.filter.EOLConvertingOutputStream

const val EXTRA_SYNC_KEY = "EXTRA_SYNC_KEY"
const val SYNC_CLASS_EMAIL = "Email"
const val SYNC_OPTION_MIME_SUPPORT_FULL = 2
const val SYNC_BODY_PREF_TYPE_MIME = 4
const val SYNC_EMAIL_FLAG_STATUS_FLAG_SET = 2

const val FILTER_ALL = 0
const val FILTER_1_DAY = 1
const val FILTER_3_DAYS = 2
const val FILTER_1_WEEK = 3
const val FILTER_2_WEEKS = 4
const val FILTER_1_MONTH = 5

class SyncCommand(private val client: EasClient,
                  private val provisionManager: EasProvisionManager,
                  private val backendStorage: BackendStorage) {
    fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        val data = when (flag) {
            Flag.SEEN -> SyncData(emailRead = if (newState) 1 else 0)
            Flag.FLAGGED -> SyncData(emailFlag = EmailFlag(if (newState) SYNC_EMAIL_FLAG_STATUS_FLAG_SET else 0))
            else -> return
        }

        executeSyncCommand(folderServerId, SyncCommands(
                change = messageServerIds.map {
                    SyncItem(serverId = it, data = data)
                }
        ))
    }

    fun fetch(folderServerId: String, messageServerId: String): EasMessage {
        val collection = executeSyncCommand(folderServerId,
                SyncCommands(fetch = listOf(
                        SyncItem(serverId = messageServerId)
                )),
                SyncOptions(
                        mimeSupport = SYNC_OPTION_MIME_SUPPORT_FULL,
                        bodyPreference = SyncBodyPreference(SYNC_BODY_PREF_TYPE_MIME)
                )
        )

        return collection.responses?.fetch?.firstOrNull()?.extractEasMessage(folderServerId)
                ?: throw MessagingException("Message not found")
    }

    fun sync(folderServerId: String, syncConfig: SyncConfig, listener: SyncListener) {
        val backendFolder = backendStorage.getFolder(folderServerId)

        var syncKey = backendFolder.loadSyncKey()

        val filterType = when (syncConfig.maximumPolledMessageAge) {
            0 -> FILTER_1_DAY
            1, 2 -> FILTER_3_DAYS
            7 -> FILTER_1_WEEK
            14 -> FILTER_2_WEEKS
            21, 28 -> FILTER_1_MONTH
            else -> FILTER_ALL
        }

        var messagesLoaded = 0

        provisionManager.ensureProvisioned {
            if (syncKey == INITIAL_SYNC_KEY) {
                val syncResponse = client.sync(Sync(SyncCollections(SyncCollection(
                        SYNC_CLASS_EMAIL,
                        syncKey,
                        folderServerId))))

                syncKey = syncResponse.collections?.collection?.syncKey
                        ?: throw MessagingException("Couldn't sync messages")
            }
            while (true) {
                val syncResponse = client.sync(Sync(SyncCollections(
                        SyncCollection(
                                SYNC_CLASS_EMAIL,
                                syncKey,
                                folderServerId,
                                deleteAsMoves = 1,
                                options = SyncOptions(
                                        mimeSupport = SYNC_OPTION_MIME_SUPPORT_FULL,
                                        bodyPreference = SyncBodyPreference(
                                                SYNC_BODY_PREF_TYPE_MIME,
                                                syncConfig.maximumAutoDownloadMessageSize),
                                        filterType = filterType
                                ),
                                getChanges = 1,
                                windowSize = 30)
                )))

                val collection = syncResponse.collections?.collection

                if (collection?.status != STATUS_OK) {
                    throw MessagingException("Couldn't execute sync command")
                }

                syncKey = collection.syncKey ?: throw MessagingException("Couldn't sync messages")

                backendFolder.storeSyncKey(syncKey)

                val commands = collection.commands
                if (commands != null) {
                    if (commands.add?.isNotEmpty() == true) {
                        for (item in commands.add) {
                            val message = item.extractEasMessage(folderServerId)

                            if (item.isTruncated()) {
                                backendFolder.savePartialMessage(message)
                            } else {
                                backendFolder.saveCompleteMessage(message)
                            }

                            listener.syncNewMessage(folderServerId, item.serverId!!, false)
                        }
                        messagesLoaded += commands.add.size
                    }

                    if (commands.delete?.isNotEmpty() == true && syncConfig.syncRemoteDeletions) {
                        backendFolder.destroyMessages(commands.delete.map { it.serverId!! })

                        for (item in commands.delete) {
                            listener.syncRemovedMessage(folderServerId, item.serverId!!)
                        }
                    }

                    if (commands.change?.isNotEmpty() == true) {
                        for (item in commands.change) {
                            var flagChanged = false

                            item.data?.emailRead?.let {
                                backendFolder.setMessageFlag(item.serverId!!, Flag.SEEN, it == 1)
                                flagChanged = true
                            }

                            item.data?.emailFlag?.let {
                                backendFolder.setMessageFlag(item.serverId!!, Flag.FLAGGED, it.status == SYNC_EMAIL_FLAG_STATUS_FLAG_SET)
                                flagChanged = true
                            }

                            if (flagChanged) {
                                listener.syncFlagChanged(folderServerId, item.serverId!!)
                            }
                        }
                    }
                }

                if (collection.moreAvailable != true) {
                    break
                }
            }

            listener.syncFinished(folderServerId, -1, messagesLoaded)
        }
    }

    private fun executeSyncCommand(folderServerId: String,
                                   commands: SyncCommands? = null,
                                   options: SyncOptions? = null): SyncCollection {
        val backendFolder = backendStorage.getFolder(folderServerId)
        var syncKey = backendFolder.loadSyncKey()

        return provisionManager.ensureProvisioned {
            val syncResponse = client.sync(Sync(SyncCollections(
                    SyncCollection(
                            SYNC_CLASS_EMAIL,
                            syncKey,
                            folderServerId,
                            deleteAsMoves = 0,
                            options = options,
                            commands = commands,
                            getChanges = 0)
            )))

            val collection = syncResponse.collections?.collection

            if (collection?.status != STATUS_OK) {
                throw MessagingException("Couldn't execute sync command")
            }

            syncKey = collection.syncKey ?: throw MessagingException("Couldn't sync messages")
            backendFolder.storeSyncKey(syncKey)
            collection
        }
    }

    private fun BackendFolder.loadSyncKey() = getFolderExtraString(EXTRA_SYNC_KEY) ?: INITIAL_SYNC_KEY
    private fun BackendFolder.storeSyncKey(syncKey: String) = setFolderExtraString(EXTRA_SYNC_KEY, syncKey)
}

fun SyncItem.isTruncated() = data!!.body!!.truncated == 1

fun SyncItem.extractEasMessage(folderServerId: String) = data!!.let {
    (it.body?.data!!.message as EasMessage).apply {
        setFolderServerId(folderServerId)
        messageId = serverId
        uid = serverId
        if (it.emailRead == 1) {
            setFlag(Flag.SEEN, true)
        }
        if (it.emailFlag?.status == SYNC_EMAIL_FLAG_STATUS_FLAG_SET) {
            setFlag(Flag.FLAGGED, true)
        }
    }
}

class EasMessageElement : StreamableElement {
    lateinit var message: Message

    fun from(message: Message) {
        this.message = message
    }

    override fun readFromStream(inputStream: InputStream) {
        message = EasMessage().apply {
            parse(inputStream)
        }
    }

    override fun writeToStream(outputStream: OutputStream) {
        val msgOut = EOLConvertingOutputStream(outputStream)
        message.writeTo(msgOut)
        msgOut.flush()
    }
}

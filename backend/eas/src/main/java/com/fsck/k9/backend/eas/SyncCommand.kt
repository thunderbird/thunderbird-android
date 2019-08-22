package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.backend.eas.dto.*
import com.fsck.k9.mail.*
import com.fsck.k9.mail.filter.EOLConvertingOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.min

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

    fun sync(folderServerId: String, syncConfig: SyncConfig, listener: SyncListener) {
        val backendFolder = backendStorage.getFolder(folderServerId)

        var syncKey = backendFolder.getFolderExtraString(EXTRA_SYNC_KEY) ?: INITIAL_SYNC_KEY

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

                syncKey = syncResponse.collections!!.collection!!.syncKey!!
            }
            while (true) {
                val syncResponse = client.sync(Sync(SyncCollections(
                        SyncCollection(
                                SYNC_CLASS_EMAIL,
                                syncKey,
                                folderServerId,
                                1,
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

                val collection = syncResponse.collections!!.collection!!

                syncKey = collection.syncKey!!
                backendFolder.setFolderExtraString(EXTRA_SYNC_KEY, syncKey)
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

    fun delete(folderServerId: String, messageServerIds: List<String>, exprune: Boolean = false) {
        executeCommands(folderServerId, SyncCommands(
                delete = messageServerIds.map { SyncItem(it) }
        ), deleteAsMoves = if (exprune) 0 else 1)
    }

    fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        val data = when (flag) {
            Flag.SEEN -> SyncData(emailRead = if (newState) 1 else 0)
            Flag.FLAGGED -> SyncData(emailFlag = EmailFlag(if (newState) SYNC_EMAIL_FLAG_STATUS_FLAG_SET else 0))
            else -> return
        }

        executeCommands(folderServerId, SyncCommands(
                change = messageServerIds.map {
                    SyncItem(serverId = it, data = data)
                }
        ))
    }

    private fun executeCommands(folderServerId: String, syncCommands: SyncCommands, deleteAsMoves: Int = 1) {
        val backendFolder = backendStorage.getFolder(folderServerId)
        val syncKey = backendFolder.getFolderExtraString(EXTRA_SYNC_KEY) ?: INITIAL_SYNC_KEY

        provisionManager.ensureProvisioned {
            val syncResponse = client.sync(Sync(
                    SyncCollections(
                            SyncCollection(
                                    SYNC_CLASS_EMAIL,
                                    syncKey,
                                    folderServerId,
                                    deleteAsMoves,
                                    commands = syncCommands
                            )
                    )
            ))

            val newSyncKey = syncResponse.collections!!.collection!!.syncKey!!
            backendFolder.setFolderExtraString(EXTRA_SYNC_KEY, newSyncKey)
        }
    }
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


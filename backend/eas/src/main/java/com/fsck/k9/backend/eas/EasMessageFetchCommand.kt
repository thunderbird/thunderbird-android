package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage

class EasMessageFetchCommand(private val client: EasClient,
                             private val provisionManager: EasProvisionManager,
                             private val backendStorage: BackendStorage) {

    fun fetch(folderServerId: String, messageServerId: String): EasMessage {
        val backendFolder = backendStorage.getFolder(folderServerId)

        val syncKey = backendFolder.getFolderExtraString(EXTRA_SYNC_KEY) ?: "0"
        println(syncKey)

        return provisionManager.ensureProvisioned {
            val syncResponse = client.sync(Sync(SyncCollections(SyncCollection("Email",
                    syncKey,
                    folderServerId,
                    options = SyncOptions(
                            mimeSupport = 2,
                            bodyPreference = SyncBodyPreference(4)
                    ),
                    commands = SyncCommands(fetch = listOf(
                            SyncItem(messageServerId)
                    ))))))

            val newSyncKey = syncResponse!!.collections!!.collection!!.syncKey!!
            backendFolder.setFolderExtraString(EXTRA_SYNC_KEY, newSyncKey)
            val responses = syncResponse.collections!!.collection!!.responses

            val message = EasMessage(EasFolder(folderServerId))
            message.uid = messageServerId
            message.messageId = messageServerId
            println(responses)
            message.parse(responses!!.item.first().data!!.body!!.data!!.byteInputStream())

            message
        }
    }
}

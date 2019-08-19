package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.mail.MessagingException

class EasMessageFetchCommand(private val client: EasClient,
                             private val provisionManager: EasProvisionManager,
                             private val backendStorage: BackendStorage) {

    fun fetch(folderServerId: String, messageServerId: String): EasMessage {
        val backendFolder = backendStorage.getFolder(folderServerId)

        val syncKey = backendFolder.getFolderExtraString(EXTRA_SYNC_KEY) ?: INITIAL_SYNC_KEY

        return provisionManager.ensureProvisioned {
            val syncResponse = client.sync(Sync(
                    SyncCollections(
                            SyncCollection("Email",
                                    syncKey,
                                    folderServerId,
                                    options = SyncOptions(
                                            mimeSupport = 2,
                                            bodyPreference = SyncBodyPreference(4)
                                    ),
                                    commands = SyncCommands(fetch = listOf(
                                            SyncItem(messageServerId)
                                    ))
                            )
                    )
            ))

            if (syncResponse.status != STATUS_OK) {
                throw MessagingException("Couldn't sync messages")
            }
            val collection = syncResponse.collections!!.collection!!

            val newSyncKey = collection.syncKey!!
            backendFolder.setFolderExtraString(EXTRA_SYNC_KEY, newSyncKey)

            val message = collection.responses?.fetch?.firstOrNull()?.getMessage(EasFolder(folderServerId))
                    ?: throw MessagingException("Message not found")
            message
        }
    }
}

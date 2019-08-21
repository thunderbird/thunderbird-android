package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.eas.dto.*
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException

class MessageUploadCommand(private val client: EasClient,
                           private val provisionManager: EasProvisionManager,
                           private val backendStorage: BackendStorage) {

    fun upload(folderServerId: String, message: Message): String {
        val backendFolder = backendStorage.getFolder(folderServerId)

        val syncKey = backendFolder.getFolderExtraString(EXTRA_SYNC_KEY) ?: INITIAL_SYNC_KEY

        return provisionManager.ensureProvisioned {
            val syncResponse = client.sync(Sync(
                    SyncCollections(
                            SyncCollection(SYNC_CLASS_EMAIL,
                                    syncKey,
                                    folderServerId,
                                    commands = SyncCommands(add = listOf(
                                            SyncItem(clientId = "tmpId", data = SyncData(body = Body(data = EasMessageElement().apply { from(message) })))
                                    ))
                            )
                    )
            ))

            val collection = syncResponse.collections?.collection

            if (collection?.status != STATUS_OK) {
                throw MessagingException("Couldn't upload message")
            }

            val newSyncKey = collection.syncKey!!
            backendFolder.setFolderExtraString(EXTRA_SYNC_KEY, newSyncKey)

            println(syncResponse)

            val serverId = collection.commands?.add?.firstOrNull()?.serverId
                    ?: throw MessagingException("Couldn't upload message")
            serverId
        }
    }
}

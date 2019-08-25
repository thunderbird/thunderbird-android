package com.fsck.k9.backend.eas

import com.fsck.k9.backend.eas.dto.MoveItem
import com.fsck.k9.backend.eas.dto.MoveItems
import com.fsck.k9.mail.MessagingException


class MessageMoveCommand(private val client: EasClient,
                         private val provisionManager: EasProvisionManager) {
    val STATUS_OK = 3

    fun moveMessages(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String> {
        return provisionManager.ensureProvisioned {
            val moveResponse = client.moveItems(MoveItems(
                    moveItems = messageServerIds.map {
                        MoveItem(it, sourceFolderServerId, targetFolderServerId)
                    }
            ))

            if (moveResponse.response == null) {
                throw MessagingException("Couldn't move messages")
            }

            moveResponse.response.filter { it.status == STATUS_OK }.map { it.srcMessageId to it.destMessageId!! }.toMap()
        }
    }
}

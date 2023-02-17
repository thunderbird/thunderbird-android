package com.fsck.k9.backend.jmap

import com.fsck.k9.logging.Timber
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse
import rs.ltt.jmap.common.util.Patches

class CommandMove(
    private val jmapClient: JmapClient,
    private val accountId: String,
) {
    fun moveMessages(targetFolderServerId: String, messageServerIds: List<String>) {
        Timber.v("Moving %d messages to %s", messageServerIds.size, targetFolderServerId)

        val mailboxPatch = Patches.set("mailboxIds", mapOf(targetFolderServerId to true))
        updateEmails(messageServerIds, mailboxPatch)
    }

    fun moveMessagesAndMarkAsRead(targetFolderServerId: String, messageServerIds: List<String>) {
        Timber.v("Moving %d messages to %s and marking them as read", messageServerIds.size, targetFolderServerId)

        val mailboxPatch = Patches.builder()
            .set("mailboxIds", mapOf(targetFolderServerId to true))
            .set("keywords/\$seen", true)
            .build()
        updateEmails(messageServerIds, mailboxPatch)
    }

    fun copyMessages(targetFolderServerId: String, messageServerIds: List<String>) {
        Timber.v("Copying %d messages to %s", messageServerIds.size, targetFolderServerId)

        val mailboxPatch = Patches.set("mailboxIds/$targetFolderServerId", true)
        updateEmails(messageServerIds, mailboxPatch)
    }

    private fun updateEmails(messageServerIds: List<String>, patch: Map<String, Any>?) {
        val session = jmapClient.session.get()
        val maxObjectsInSet = session.maxObjectsInSet

        messageServerIds.chunked(maxObjectsInSet).forEach { emailIds ->
            val updates = emailIds.map { emailId ->
                emailId to patch
            }.toMap()

            val setEmailCall = jmapClient.call(
                SetEmailMethodCall.builder()
                    .accountId(accountId)
                    .update(updates)
                    .build(),
            )

            setEmailCall.getMainResponseBlocking<SetEmailMethodResponse>()
        }
    }
}

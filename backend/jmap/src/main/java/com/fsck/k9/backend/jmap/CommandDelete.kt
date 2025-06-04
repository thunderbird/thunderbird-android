package com.fsck.k9.backend.jmap

import net.thunderbird.core.logging.legacy.Log
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.common.Request.Invocation.ResultReference
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall
import rs.ltt.jmap.common.method.response.email.QueryEmailMethodResponse
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse

class CommandDelete(
    private val jmapClient: JmapClient,
    private val accountId: String,
) {
    fun deleteMessages(messageServerIds: List<String>) {
        Log.v("Deleting messages %s", messageServerIds)

        val session = jmapClient.session.get()
        val maxObjectsInSet = session.maxObjectsInSet

        messageServerIds.chunked(maxObjectsInSet).forEach { emailIds ->
            val setEmailCall = jmapClient.call(
                SetEmailMethodCall.builder()
                    .accountId(accountId)
                    .destroy(emailIds.toTypedArray())
                    .build(),
            )

            setEmailCall.getMainResponseBlocking<SetEmailMethodResponse>()
        }
    }

    fun deleteAllMessages(folderServerId: String) {
        Log.d("Deleting all messages from %s", folderServerId)

        val session = jmapClient.session.get()
        val limit = session.maxObjectsInSet.coerceAtMost(MAX_CHUNK_SIZE).toLong()

        do {
            Log.v("Trying to delete up to %d messages from %s", limit, folderServerId)
            val multiCall = jmapClient.newMultiCall()

            val queryEmailCall = multiCall.call(
                QueryEmailMethodCall.builder()
                    .accountId(accountId)
                    .filter(EmailFilterCondition.builder().inMailbox(folderServerId).build())
                    .calculateTotal(true)
                    .limit(limit)
                    .build(),
            )

            val setEmailCall = multiCall.call(
                SetEmailMethodCall.builder()
                    .accountId(accountId)
                    .destroyReference(queryEmailCall.createResultReference(ResultReference.Path.IDS))
                    .build(),
            )

            multiCall.execute()

            val queryEmailResponse = queryEmailCall.getMainResponseBlocking<QueryEmailMethodResponse>()
            val numberOfReturnedEmails = queryEmailResponse.ids.size
            val totalNumberOfEmails = queryEmailResponse.total ?: error("Server didn't return property 'total'")

            setEmailCall.getMainResponseBlocking<SetEmailMethodResponse>()

            Log.v("Deleted %d messages from %s", numberOfReturnedEmails, folderServerId)
        } while (totalNumberOfEmails > numberOfReturnedEmails)
    }
}

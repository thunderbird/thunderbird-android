package com.fsck.k9.backend.jmap

import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.Flag
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall
import rs.ltt.jmap.common.method.response.email.QueryEmailMethodResponse
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse
import rs.ltt.jmap.common.util.Patches

class CommandSetFlag(
    private val jmapClient: JmapClient,
    private val accountId: String,
) {
    fun setFlag(messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        if (newState) {
            Timber.v("Setting flag %s for messages %s", flag, messageServerIds)
        } else {
            Timber.v("Removing flag %s for messages %s", flag, messageServerIds)
        }

        val keyword = flag.toKeyword()
        val keywordsPatch = if (newState) {
            Patches.set("keywords/$keyword", true)
        } else {
            Patches.remove("keywords/$keyword")
        }

        val session = jmapClient.session.get()
        val maxObjectsInSet = session.maxObjectsInSet

        messageServerIds.chunked(maxObjectsInSet).forEach { emailIds ->
            val updates = emailIds.map { emailId ->
                emailId to keywordsPatch
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

    fun markAllAsRead(folderServerId: String) {
        Timber.d("Marking all messages in %s as read", folderServerId)

        val keywordsPatch = Patches.set("keywords/\$seen", true)

        val session = jmapClient.session.get()
        val limit = minOf(MAX_CHUNK_SIZE, session.maxObjectsInSet).toLong()

        do {
            Timber.v("Trying to mark up to %d messages in %s as read", limit, folderServerId)

            val queryEmailCall = jmapClient.call(
                QueryEmailMethodCall.builder()
                    .accountId(accountId)
                    .filter(
                        EmailFilterCondition.builder()
                            .inMailbox(folderServerId)
                            .notKeyword("\$seen")
                            .build(),
                    )
                    .calculateTotal(true)
                    .limit(limit)
                    .build(),
            )

            val queryEmailResponse = queryEmailCall.getMainResponseBlocking<QueryEmailMethodResponse>()
            val numberOfReturnedEmails = queryEmailResponse.ids.size
            val totalNumberOfEmails = queryEmailResponse.total ?: error("Server didn't return property 'total'")

            if (numberOfReturnedEmails == 0) {
                Timber.v("There were no messages in %s to mark as read", folderServerId)
            } else {
                val updates = queryEmailResponse.ids.map { emailId ->
                    emailId to keywordsPatch
                }.toMap()

                val setEmailCall = jmapClient.call(
                    SetEmailMethodCall.builder()
                        .accountId(accountId)
                        .update(updates)
                        .build(),
                )

                setEmailCall.getMainResponseBlocking<SetEmailMethodResponse>()

                Timber.v("Marked %d messages in %s as read", numberOfReturnedEmails, folderServerId)
            }
        } while (totalNumberOfEmails > numberOfReturnedEmails)
    }

    private fun Flag.toKeyword(): String = when (this) {
        Flag.SEEN -> "\$seen"
        Flag.FLAGGED -> "\$flagged"
        Flag.DRAFT -> "\$draft"
        Flag.ANSWERED -> "\$answered"
        Flag.FORWARDED -> "\$forwarded"
        else -> error("Unsupported flag: $name")
    }
}

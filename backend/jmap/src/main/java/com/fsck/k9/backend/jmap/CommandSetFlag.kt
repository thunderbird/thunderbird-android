package com.fsck.k9.backend.jmap

import com.fsck.k9.mail.Flag
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse
import rs.ltt.jmap.common.util.Patches
import timber.log.Timber

class CommandSetFlag(
    private val jmapClient: JmapClient,
    private val accountId: String
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
                    .build()
            )

            setEmailCall.getMainResponseBlocking<SetEmailMethodResponse>()
        }
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

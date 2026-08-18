package com.fsck.k9.message

import com.fsck.k9.helper.ReplyToParser
import com.fsck.k9.mail.Message
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.message.reader.api.strategy.ReplyActionStrategy

/**
 * Figures out which reply actions are available to the user.
 */
class LegacyReplyActionStrategy(private val replyRoParser: ReplyToParser) :
    ReplyActionStrategy<LegacyAccountDto, Message> {
    override fun getReplyActions(
        account: LegacyAccountDto,
        message: Message,
    ): net.thunderbird.feature.mail.message.reader.api.domain.ReplyActions {
        val recipientsToReplyTo = replyRoParser.getRecipientsToReplyTo(message, account)
        val recipientsToReplyAllTo = replyRoParser.getRecipientsToReplyAllTo(message, account)

        val replyToAllCount = recipientsToReplyAllTo.to.size + recipientsToReplyAllTo.cc.size
        return if (replyToAllCount <= 1) {
            if (recipientsToReplyTo.to.isEmpty()) {
                ReplyActions(defaultAction = null)
            } else {
                ReplyActions(defaultAction = ReplyAction.REPLY)
            }
        } else {
            ReplyActions(defaultAction = ReplyAction.REPLY, additionalActions = listOf(ReplyAction.REPLY_ALL))
        }
    }
}

@Deprecated(
    message = "Use net.thunderbird.feature.mail.message.reader.api.domain.ReplyActions instead.",
    replaceWith = ReplaceWith(
        expression = "ReplyActions",
        "net.thunderbird.feature.mail.message.reader.api.domain.ReplyActions",
    ),
)
typealias ReplyActions = net.thunderbird.feature.mail.message.reader.api.domain.ReplyActions

@Deprecated(
    message = "Use net.thunderbird.feature.mail.message.reader.api.domain.ReplyAction instead.",
    replaceWith = ReplaceWith(
        expression = "ReplyAction",
        "net.thunderbird.feature.mail.message.reader.api.domain.ReplyAction",
    ),
)
typealias ReplyAction = net.thunderbird.feature.mail.message.reader.api.domain.ReplyAction

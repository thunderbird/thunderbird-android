package net.thunderbird.feature.mail.message.reader.api.strategy

import net.thunderbird.feature.account.Account
import net.thunderbird.feature.mail.message.reader.api.domain.ReplyActions

/**
 * Figures out which reply actions are available to the user.
 */
fun interface ReplyActionStrategy<TAccount : Account, TMessage> {
    fun getReplyActions(account: TAccount, message: TMessage): ReplyActions
}

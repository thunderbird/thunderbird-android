package com.fsck.k9.message

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.helper.ReplyToParser
import com.fsck.k9.mail.testing.message.buildMessage
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import org.junit.Test

private const val IDENTITY_EMAIL_ADDRESS = "myself@domain.example"

class ReplyActionStrategyTest {
    private val account = createAccount()
    private val replyActionStrategy = ReplyActionStrategy(ReplyToParser())

    @Test
    fun `message sent to only our identity`() {
        val message = buildMessage {
            header("From", "sender@domain.example")
            header("To", IDENTITY_EMAIL_ADDRESS)
        }

        val replyActions = replyActionStrategy.getReplyActions(account, message)

        assertThat(replyActions.defaultAction).isEqualTo(ReplyAction.REPLY)
        assertThat(replyActions.additionalActions).isEmpty()
    }

    @Test
    fun `message sent to our identity and others`() {
        val message = buildMessage {
            header("From", "sender@domain.example")
            header("To", "$IDENTITY_EMAIL_ADDRESS, other@domain.example")
        }

        val replyActions = replyActionStrategy.getReplyActions(account, message)

        assertThat(replyActions.defaultAction).isEqualTo(ReplyAction.REPLY)
        assertThat(replyActions.additionalActions).containsExactly(ReplyAction.REPLY_ALL)
    }

    @Test
    fun `message sent to our identity and others (CC)`() {
        val message = buildMessage {
            header("From", "sender@domain.example")
            header("Cc", "$IDENTITY_EMAIL_ADDRESS, other@domain.example")
        }

        val replyActions = replyActionStrategy.getReplyActions(account, message)

        assertThat(replyActions.defaultAction).isEqualTo(ReplyAction.REPLY)
        assertThat(replyActions.additionalActions).containsExactly(ReplyAction.REPLY_ALL)
    }

    @Test
    fun `message sent to our identity and others (To+CC)`() {
        val message = buildMessage {
            header("From", "sender@domain.example")
            header("To", IDENTITY_EMAIL_ADDRESS)
            header("Cc", "other@domain.example")
        }

        val replyActions = replyActionStrategy.getReplyActions(account, message)

        assertThat(replyActions.defaultAction).isEqualTo(ReplyAction.REPLY)
        assertThat(replyActions.additionalActions).containsExactly(ReplyAction.REPLY_ALL)
    }

    @Test
    fun `message sent to our identity and others (CC+To)`() {
        val message = buildMessage {
            header("From", "sender@domain.example")
            header("To", "other@domain.example")
            header("Cc", IDENTITY_EMAIL_ADDRESS)
        }

        val replyActions = replyActionStrategy.getReplyActions(account, message)

        assertThat(replyActions.defaultAction).isEqualTo(ReplyAction.REPLY)
        assertThat(replyActions.additionalActions).containsExactly(ReplyAction.REPLY_ALL)
    }

    @Test
    fun `message where neither sender nor recipient addresses belong to account`() {
        val message = buildMessage {
            header("From", "sender@domain.example")
            header("To", "recipient@domain.example")
        }

        val replyActions = replyActionStrategy.getReplyActions(account, message)

        assertThat(replyActions.defaultAction).isEqualTo(ReplyAction.REPLY)
        assertThat(replyActions.additionalActions).containsExactly(ReplyAction.REPLY_ALL)
    }

    @Test
    fun `message without any sender or recipient headers`() {
        val message = buildMessage {}

        val replyActions = replyActionStrategy.getReplyActions(account, message)

        assertThat(replyActions.defaultAction).isNull()
        assertThat(replyActions.additionalActions).isEmpty()
    }

    private fun createAccount(): LegacyAccountDto {
        return LegacyAccountDto("00000000-0000-4000-0000-000000000000").apply {
            identities += Identity(name = "Myself", email = IDENTITY_EMAIL_ADDRESS)
        }
    }
}

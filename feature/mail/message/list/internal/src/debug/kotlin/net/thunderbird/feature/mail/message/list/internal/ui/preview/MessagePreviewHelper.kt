package net.thunderbird.feature.mail.message.list.internal.ui.preview

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Avatar
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

internal object MessagePreviewHelper {
    fun createMessage(
        id: String = "msg-1",
        state: MessageItemUi.State = MessageItemUi.State.Unread,
        senderName: String = "Alice Johnson <alice@example.com>",
        subject: String = "Weekly Team Sync",
        excerpt: String = "Hi team, let's discuss the progress on the new feature and plan next steps.",
        formattedReceivedAt: String = "2:34 PM",
        starred: Boolean = false,
        answered: Boolean = false,
        forwarded: Boolean = false,
        encrypted: Boolean = false,
        selected: Boolean = false,
        hasAttachments: Boolean = false,
        threadCount: Int = 0,
        messageAccount: Account = AccountPreviewHelper.account,
    ) = MessageItemUi(
        state = state,
        id = id,
        folderId = "folder-inbox",
        account = messageAccount,
        senders = ComposedAddressUi(
            displayName = senderName,
            avatar = Avatar.Monogram(senderName.first().toString()),
            color = Color.Green,
        ),
        subject = subject,
        excerpt = excerpt,
        formattedReceivedAt = formattedReceivedAt,
        hasAttachments = hasAttachments,
        starred = starred,
        encrypted = encrypted,
        answered = answered,
        forwarded = forwarded,
        selected = selected,
        threadCount = threadCount,
    )

    val sampleMessages = persistentListOf(
        createMessage(
            id = "msg-1",
            state = MessageItemUi.State.Unread,
            senderName = "Alice Johnson <alice@example.com>",
            subject = "Weekly Team Sync",
            excerpt = "Hi team, let's discuss the progress on the new feature and plan next steps for the sprint.",
            formattedReceivedAt = "2:34 PM",
            starred = true,
        ),
        createMessage(
            id = "msg-2",
            state = MessageItemUi.State.Read,
            senderName = "Bob Smith <bob@example.com>",
            subject = "Re: Project Proposal",
            excerpt = "Looks good! I've reviewed the document and left some comments.",
            formattedReceivedAt = "Yesterday",
            answered = true,
        ),
        createMessage(
            id = "msg-3",
            state = MessageItemUi.State.New,
            senderName = "Carol Davis <carol@example.com>",
            subject = "Invoice #2024-0142",
            excerpt = "Please find attached the invoice for the consulting services provided in January.",
            formattedReceivedAt = "Monday",
            hasAttachments = true,
        ),
        createMessage(
            id = "msg-4",
            state = MessageItemUi.State.Read,
            senderName = "David Lee <david@example.com>",
            subject = "Fwd: Conference Registration",
            excerpt = "Forwarding the registration confirmation for next month's tech conference.",
            formattedReceivedAt = "Jan 15",
            forwarded = true,
        ),
        createMessage(
            id = "msg-5",
            state = MessageItemUi.State.Unread,
            senderName = "Eve Martinez <eve@secure.org>",
            subject = "Encrypted: Quarterly Report",
            excerpt = "The quarterly financial report is ready for your review.",
            formattedReceivedAt = "Jan 10",
            encrypted = true,
            starred = true,
        ),
    )

    val selectedMessages = persistentListOf(
        createMessage(id = "msg-1", selected = true, starred = true),
        createMessage(
            id = "msg-2",
            state = MessageItemUi.State.Read,
            senderName = "Bob Smith <bob@example.com>",
            subject = "Re: Project Proposal",
            excerpt = "Looks good!",
            formattedReceivedAt = "Yesterday",
            selected = true,
        ),
        createMessage(
            id = "msg-3",
            state = MessageItemUi.State.Read,
            senderName = "Carol Davis <carol@example.com>",
            subject = "Invoice #2024-0142",
            excerpt = "Please find attached the invoice.",
            formattedReceivedAt = "Monday",
        ),
    )

    val conversationMessages = persistentListOf(
        createMessage(
            id = "msg-thread-1",
            subject = "Re: Project Architecture Discussion",
            excerpt = "I agree with your approach. Let's schedule a follow-up.",
            formattedReceivedAt = "3:15 PM",
            threadCount = 2,
        ),
        createMessage(
            id = "msg-2",
            state = MessageItemUi.State.Read,
            senderName = "Carol Davis <carol@example.com>",
            subject = "Meeting Notes",
            excerpt = "Here are the notes from today's standup.",
            formattedReceivedAt = "11:00 AM",
        ),
    )

    val multiAccountMessages = persistentListOf(
        createMessage(
            id = "msg-acc1-1",
            senderName = "Alice Johnson <alice@work.com>",
            subject = "Q4 Planning",
            excerpt = "Let's align on Q4 priorities.",
            formattedReceivedAt = "10:00 AM",
            messageAccount = AccountPreviewHelper.account,
        ),
        createMessage(
            id = "msg-acc2-1",
            state = MessageItemUi.State.New,
            senderName = "Personal Newsletter <news@personal.com>",
            subject = "Your Weekly Digest",
            excerpt = "Top stories from this week.",
            formattedReceivedAt = "9:00 AM",
            messageAccount = AccountPreviewHelper.secondAccount,
        ),
        createMessage(
            id = "msg-acc1-2",
            state = MessageItemUi.State.Read,
            senderName = "Bob Smith <bob@work.com>",
            subject = "Code Review Request",
            excerpt = "Could you review PR #345?",
            formattedReceivedAt = "Yesterday",
            messageAccount = AccountPreviewHelper.account,
        ),
    )
}

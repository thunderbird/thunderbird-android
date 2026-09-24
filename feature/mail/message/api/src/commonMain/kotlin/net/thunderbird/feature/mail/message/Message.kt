package net.thunderbird.feature.mail.message

import kotlinx.datetime.LocalDateTime
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.piisafe.annotation.PiiSafe

/**
 * A single email message, independent of the mail protocol or storage backend that produced it.
 *
 * @property id Local identifier, unique within this app's storage.
 * @property serverId Identifier assigned by the mail server, e.g. an IMAP UID.
 * @property accountId Account this message belongs to.
 * @property folderId Folder this message is currently in.
 * @property threadRoot Id of the first message in this message's thread, or null if this message starts the thread.
 * @property receivedAt Time the server received this message.
 * @property envelope Subject, addresses, and dates for this message.
 * @property headers Threading headers for this message.
 * @property body Message content. Null if only the envelope has been downloaded so far.
 * @property downloadState How much of this message has been downloaded so far.
 * @property flags Flags and keywords set on this message.
 * @property attachments Attachments, or null if the message structure has not been fetched yet.
 * @property security Encryption and signature state. Unknown until the body has been inspected.
 * @property size Total message size, or null if unknown.
 */
@PiiSafe.HasPii
data class Message(
    val id: MessageId?,
    @get:PiiSafe.Mask
    val serverId: MessageServerId?,
    @get:PiiSafe.Mask
    val accountId: AccountId,
    val folderId: FolderId?,
    val threadRoot: ThreadId?,
    val receivedAt: LocalDateTime,
    val envelope: MessageEnvelope,
    val headers: MessageHeaders,
    val body: MessageBody?,
    val downloadState: MessageDownloadState,
    val flags: Set<MessageFlag> = emptySet(),
    val attachments: List<MessageAttachment>? = null,
    val security: MessageSecurity = MessageSecurity(),
    val size: MessageSize? = null,
    @get:PiiSafe.Hide
    val source: MessageSource? = null,
)

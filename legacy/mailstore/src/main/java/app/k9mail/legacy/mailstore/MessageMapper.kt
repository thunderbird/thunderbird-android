package app.k9mail.legacy.mailstore

import app.k9mail.legacy.message.extractors.PreviewResult
import com.fsck.k9.mail.Address

fun interface MessageMapper<T> {
    fun map(message: MessageDetailsAccessor): T
}

interface MessageDetailsAccessor {
    val id: Long
    val messageServerId: String
    val folderId: Long
    val fromAddresses: List<Address>
    val toAddresses: List<Address>
    val ccAddresses: List<Address>
    val messageDate: Long
    val internalDate: Long
    val subject: String?
    val preview: PreviewResult
    val isRead: Boolean
    val isStarred: Boolean
    val isAnswered: Boolean
    val isForwarded: Boolean
    val hasAttachments: Boolean
    val threadRoot: Long
    val threadCount: Int
    val size: Long
}

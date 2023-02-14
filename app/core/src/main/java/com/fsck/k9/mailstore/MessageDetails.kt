package com.fsck.k9.mailstore

import com.fsck.k9.mail.Address
import java.util.Date

data class MessageDetails(
    val date: MessageDate,
    val from: List<Address>,
    val sender: Address?,
    val replyTo: List<Address>,
    val to: List<Address>,
    val cc: List<Address>,
    val bcc: List<Address>,
)

sealed interface MessageDate {
    data class ValidDate(val date: Date) : MessageDate

    data class InvalidDate(val dateHeader: String) : MessageDate

    object MissingDate : MessageDate
}

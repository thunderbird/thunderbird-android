package net.thunderbird.feature.mail.message.reader.api.domain

data class ReplyActions(
    val defaultAction: ReplyAction?,
    val additionalActions: List<ReplyAction> = emptyList(),
)

enum class ReplyAction {
    REPLY,
    REPLY_ALL,
}

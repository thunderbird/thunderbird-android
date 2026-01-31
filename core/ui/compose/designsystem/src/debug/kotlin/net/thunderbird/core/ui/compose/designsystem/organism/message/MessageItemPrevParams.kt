package net.thunderbird.core.ui.compose.designsystem.organism.message

internal data class MessageItemPrevParams(
    val sender: String,
    val subject: String,
    val preview: String,
    val hasAttachments: Boolean,
    val selected: Boolean,
    val favourite: Boolean = false,
    val threadCount: Int = 0,
    val swapSenderWithSubject: Boolean = false,
    val receivedAt: String = "12:34",
)

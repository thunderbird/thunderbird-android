package net.thunderbird.core.ui.compose.designsystem.organism.message

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal data class MessageItemPrevParams(
    val sender: String,
    val subject: String,
    val preview: String,
    val hasAttachments: Boolean,
    val selected: Boolean,
    val favourite: Boolean = false,
    val threadCount: Int = 0,
    val swapSenderWithSubject: Boolean = false,
    val receivedAt: LocalDateTime = @OptIn(ExperimentalTime::class) Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault()),
)

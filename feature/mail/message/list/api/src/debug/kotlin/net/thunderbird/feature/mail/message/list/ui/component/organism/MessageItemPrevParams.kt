package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListDateTimeFormat
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageBadgeStyle
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemTrailingElement
import net.thunderbird.feature.mail.message.list.ui.state.Avatar
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressStyle

internal data class MessageItemPrevParams(
    val previewName: String,
    val sender: String,
    val senderStyles: ImmutableList<ComposedAddressStyle> = persistentListOf(),
    val subject: String,
    val excerpt: String,
    val hasAttachments: Boolean,
    val selected: Boolean,
    val favourite: Boolean = false,
    val threadCount: Int = 0,
    val senderAboveSubject: Boolean = true,
    val receivedAt: String = "12:34",
    val maxExcerptLines: Int = 2,
    val badgeStyle: MessageBadgeStyle? = null,
    val avatar: Avatar? = null,
    val avatarColor: Color? = null,
    val accountColor: Color = Color.DarkGray,
    val trailingElements: ImmutableList<MessageItemTrailingElement> = persistentListOf(
        MessageItemTrailingElement.FavouriteIconButton(
            favourite = favourite,
        ),
    ),
    val answered: Boolean = false,
    val forwarded: Boolean = false,
    val showCorrespondentName: Boolean = true,
    val showAvatar: Boolean = true,
    val showFavorite: Boolean = true,
    val dateTimeFormat: MessageListDateTimeFormat = MessageListDateTimeFormat.Contextual,
)

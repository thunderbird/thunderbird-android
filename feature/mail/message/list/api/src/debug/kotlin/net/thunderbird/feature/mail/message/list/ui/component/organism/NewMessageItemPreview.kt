package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemAccountIndicator
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemTrailingElement
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Avatar
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressStyle
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

private class NewMessageItemPrevParamCol : CollectionPreviewParameterProvider<MessageItemPrevParams>(
    collection = listOf(
        MessageItemPrevParams(
            sender = "Cynthia Alvarez",
            senderStyles = ComposedAddressStyle.AllBold,
            subject = "Laptop not booting after Windows update",
            excerpt = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = false,
            selected = false,
            favourite = false,
            threadCount = 0,
            senderAboveSubject = true,
            avatar = Avatar.Monogram("CA"),
        ),
        MessageItemPrevParams(
            sender = "Mason Tran, Me, Ryan Thomas",
            senderStyles = persistentListOf(
                ComposedAddressStyle.Bold(0, 11),
            ),
            subject = "Follow-up on gaming PC build specs",
            excerpt = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = false,
            selected = false,
            favourite = false,
            threadCount = 10,
            senderAboveSubject = true,
            avatar = Avatar.Monogram("MT"),
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = true,
            selected = false,
            favourite = false,
            threadCount = 10,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            sender = LoremIpsum(words = 100).values.joinToString(),
            senderStyles = ComposedAddressStyle.AllBold,
            subject = LoremIpsum(words = 100).values.joinToString(),
            excerpt = LoremIpsum(words = 5).values.joinToString(),
            hasAttachments = true,
            selected = false,
            favourite = false,
            threadCount = 1,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            senderStyles = ComposedAddressStyle.AllBold,
            subject = "The subject",
            excerpt = LoremIpsum(words = 10).values.joinToString(),
            hasAttachments = false,
            selected = true,
            favourite = true,
            threadCount = 10,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            senderStyles = ComposedAddressStyle.AllBold,
            subject = "The subject",
            excerpt = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            selected = true,
            favourite = true,
            threadCount = 100,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = false,
            selected = false,
            favourite = false,
            threadCount = 0,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            sender = LoremIpsum(words = 100).values.joinToString(),
            senderStyles = ComposedAddressStyle.AllBold,
            subject = LoremIpsum(words = 100).values.joinToString(),
            excerpt = LoremIpsum(words = 5).values.joinToString(),
            hasAttachments = true,
            selected = false,
            favourite = false,
            threadCount = 1,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            senderStyles = ComposedAddressStyle.AllBold,
            subject = "The subject",
            excerpt = LoremIpsum(words = 10).values.joinToString(),
            hasAttachments = false,
            selected = true,
            favourite = true,
            threadCount = 10,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            sender = "Sender Name",
            senderStyles = ComposedAddressStyle.AllBold,
            subject = "The subject",
            excerpt = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            selected = true,
            favourite = true,
            threadCount = 100,
            senderAboveSubject = true,
            maxExcerptLines = 0,
        ),
    ),
)

@Preview
@Composable
private fun PreviewDefault(
    @PreviewParameter(NewMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        NewMessageItem(
            state = MessageItemUi(
                state = MessageItemUi.State.New,
                id = "",
                account = Account(id = AccountIdFactory.create(), color = params.accountColor),
                senders = ComposedAddressUi(
                    displayName = params.sender,
                    displayNameStyles = params.senderStyles,
                    avatar = params.avatar,
                    color = params.avatarColor,
                ),
                subject = params.subject,
                excerpt = params.excerpt,
                formattedReceivedAt = params.receivedAt,
                hasAttachments = params.hasAttachments,
                starred = params.favourite,
                encrypted = params.trailingElements.any { it is MessageItemTrailingElement.EncryptedBadge },
                answered = params.answered,
                forwarded = params.forwarded,
                selected = params.selected,
                threadCount = params.threadCount,
            ),
            preferences = MessageListPreferences(
                density = UiDensity.Default,
                groupConversations = false,
                showCorrespondentNames = params.showCorrespondentName,
                showMessageAvatar = params.showAvatar,
                showFavouriteButton = params.showFavorite,
                senderAboveSubject = params.senderAboveSubject,
                excerptLines = params.maxExcerptLines,
                dateTimeFormat = params.dateTimeFormat,
            ),
            accountIndicator = MessageItemAccountIndicator(color = params.accountColor),
            onClick = { },
            onLongClick = { },
            onAvatarClick = { },
            onFavouriteChange = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
        )
    }
}

@Preview
@Composable
private fun PreviewCompact(
    @PreviewParameter(NewMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        NewMessageItem(
            state = MessageItemUi(
                state = MessageItemUi.State.New,
                id = "",
                account = Account(id = AccountIdFactory.create(), color = params.accountColor),
                senders = ComposedAddressUi(
                    displayName = params.sender,
                    displayNameStyles = params.senderStyles,
                    avatar = params.avatar,
                    color = params.avatarColor,
                ),
                subject = params.subject,
                excerpt = params.excerpt,
                formattedReceivedAt = params.receivedAt,
                hasAttachments = params.hasAttachments,
                starred = params.favourite,
                encrypted = params.trailingElements.any { it is MessageItemTrailingElement.EncryptedBadge },
                answered = params.answered,
                forwarded = params.forwarded,
                selected = params.selected,
                threadCount = params.threadCount,
            ),
            preferences = MessageListPreferences(
                density = UiDensity.Compact,
                groupConversations = false,
                showCorrespondentNames = params.showCorrespondentName,
                showMessageAvatar = params.showAvatar,
                showFavouriteButton = params.showFavorite,
                senderAboveSubject = params.senderAboveSubject,
                excerptLines = params.maxExcerptLines,
                dateTimeFormat = params.dateTimeFormat,
            ),
            accountIndicator = MessageItemAccountIndicator(color = params.accountColor),
            onClick = { },
            onLongClick = { },
            onAvatarClick = { },
            onFavouriteChange = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
        )
    }
}

@Preview
@Composable
private fun PreviewRelaxed(
    @PreviewParameter(NewMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        NewMessageItem(
            state = MessageItemUi(
                state = MessageItemUi.State.New,
                id = "",
                account = Account(id = AccountIdFactory.create(), color = params.accountColor),
                senders = ComposedAddressUi(
                    displayName = params.sender,
                    displayNameStyles = params.senderStyles,
                    avatar = params.avatar,
                    color = params.avatarColor,
                ),
                subject = params.subject,
                excerpt = params.excerpt,
                formattedReceivedAt = params.receivedAt,
                hasAttachments = params.hasAttachments,
                starred = params.favourite,
                encrypted = params.trailingElements.any { it is MessageItemTrailingElement.EncryptedBadge },
                answered = params.answered,
                forwarded = params.forwarded,
                selected = params.selected,
                threadCount = params.threadCount,
            ),
            preferences = MessageListPreferences(
                density = UiDensity.Relaxed,
                groupConversations = false,
                showCorrespondentNames = params.showCorrespondentName,
                showMessageAvatar = params.showAvatar,
                showFavouriteButton = params.showFavorite,
                senderAboveSubject = params.senderAboveSubject,
                excerptLines = params.maxExcerptLines,
                dateTimeFormat = params.dateTimeFormat,
            ),
            accountIndicator = MessageItemAccountIndicator(color = params.accountColor),
            onClick = { },
            onLongClick = { },
            onAvatarClick = { },
            onFavouriteChange = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
        )
    }
}

@Preview
@Composable
private fun PreviewDefaultWithoutAccountIndicator(
    @PreviewParameter(NewMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        NewMessageItem(
            state = MessageItemUi(
                state = MessageItemUi.State.New,
                id = "",
                account = Account(id = AccountIdFactory.create(), color = params.accountColor),
                senders = ComposedAddressUi(
                    displayName = params.sender,
                    displayNameStyles = params.senderStyles,
                    avatar = params.avatar,
                    color = params.avatarColor,
                ),
                subject = params.subject,
                excerpt = params.excerpt,
                formattedReceivedAt = params.receivedAt,
                hasAttachments = params.hasAttachments,
                starred = params.favourite,
                encrypted = params.trailingElements.any { it is MessageItemTrailingElement.EncryptedBadge },
                answered = params.answered,
                forwarded = params.forwarded,
                selected = params.selected,
                threadCount = params.threadCount,
            ),
            preferences = MessageListPreferences(
                density = UiDensity.Default,
                groupConversations = false,
                showCorrespondentNames = params.showCorrespondentName,
                showMessageAvatar = params.showAvatar,
                showFavouriteButton = params.showFavorite,
                senderAboveSubject = params.senderAboveSubject,
                excerptLines = params.maxExcerptLines,
                dateTimeFormat = params.dateTimeFormat,
            ),
            accountIndicator = null,
            onClick = { },
            onLongClick = { },
            onAvatarClick = { },
            onFavouriteChange = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
        )
    }
}

package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import kotlin.random.Random
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemAccountIndicator
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemTrailingElement
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Avatar
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

private class ReadMessageItemPrevParamCol : CollectionPreviewParameterProvider<MessageItemPrevParams>(
    collection = listOf(
        MessageItemPrevParams(
            previewName = "Single sender with monogram",
            sender = "Cynthia Alvarez",
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
            previewName = "Multiple senders threaded",
            sender = "Mason Tran, Me, Ryan Thomas",
            subject = "Follow-up on gaming PC build specs",
            excerpt = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            selected = false,
            favourite = false,
            threadCount = 10,
            senderAboveSubject = true,
            avatar = Avatar.Monogram("MT"),
        ),
        MessageItemPrevParams(
            previewName = "Threaded no excerpt",
            sender = "Mason Tran, Me, Ryan Thomas",
            subject = "Follow-up on gaming PC build specs",
            excerpt = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            selected = false,
            favourite = false,
            threadCount = 100,
            senderAboveSubject = true,
            avatar = Avatar.Monogram("MT"),
            maxExcerptLines = 0,
        ),
        MessageItemPrevParams(
            previewName = "Subject first red monogram",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = false,
            selected = false,
            favourite = false,
            threadCount = 0,
            senderAboveSubject = false,
            avatar = Avatar.Monogram("RD"),
            avatarColor = Color.Red,
        ),
        MessageItemPrevParams(
            previewName = "Subject first long text blue",
            sender = LoremIpsum(words = 100).values.joinToString(),
            subject = LoremIpsum(words = 100).values.joinToString(),
            excerpt = LoremIpsum(words = 5).values.joinToString(),
            hasAttachments = true,
            selected = false,
            favourite = false,
            threadCount = 1,
            senderAboveSubject = false,
            avatar = Avatar.Monogram("BL"),
            avatarColor = Color.Blue,
        ),
        MessageItemPrevParams(
            previewName = "Subject first selected favourite",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 10).values.joinToString(),
            hasAttachments = false,
            selected = true,
            favourite = true,
            threadCount = 10,
            senderAboveSubject = false,
        ),
        MessageItemPrevParams(
            previewName = "Subject first selected with attachment",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            selected = true,
            favourite = true,
            threadCount = 100,
            senderAboveSubject = false,
        ),
        MessageItemPrevParams(
            previewName = "Minimal no avatar",
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
            previewName = "Long sender and subject",
            sender = LoremIpsum(words = 100).values.joinToString(),
            subject = LoremIpsum(words = 100).values.joinToString(),
            excerpt = LoremIpsum(words = 5).values.joinToString(),
            hasAttachments = true,
            selected = false,
            favourite = false,
            threadCount = 1,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            previewName = "Selected favourite threaded",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 10).values.joinToString(),
            hasAttachments = false,
            selected = true,
            favourite = true,
            threadCount = 10,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            previewName = "Selected favourite with attachment",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            selected = true,
            favourite = true,
            threadCount = 100,
            senderAboveSubject = true,
        ),
        MessageItemPrevParams(
            previewName = "Icon avatar no excerpt",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = "",
            hasAttachments = true,
            threadCount = Random.nextInt(2, 100),
            selected = false,
            receivedAt = "12:34",
            maxExcerptLines = 0,
            avatar = Avatar.Icon(imageVector = Icons.Outlined.Bank),
            avatarColor = Color.DarkGray,
        ),
        MessageItemPrevParams(
            previewName = "Long excerpt five lines",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 100).values.joinToString { it.replace("\n", "") },
            hasAttachments = true,
            threadCount = Random.nextInt(2, 100),
            selected = false,
            receivedAt = "12:34",
            maxExcerptLines = 5,
        ),
    ),
) {
    override fun getDisplayName(index: Int): String = values.elementAt(index).previewName
}

@Preview
@Composable
private fun PreviewDefault(
    @PreviewParameter(ReadMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        ReadMessageItem(
            state = MessageItemUi(
                state = MessageItemUi.State.Read,
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
                encrypted = params.trailingElements.any { it == MessageItemTrailingElement.EncryptedBadge },
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
    @PreviewParameter(ReadMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        ReadMessageItem(
            state = MessageItemUi(
                state = MessageItemUi.State.Read,
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
                encrypted = params.trailingElements.any { it == MessageItemTrailingElement.EncryptedBadge },
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
    @PreviewParameter(ReadMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        ReadMessageItem(
            state = MessageItemUi(
                state = MessageItemUi.State.Read,
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
                encrypted = params.trailingElements.any { it == MessageItemTrailingElement.EncryptedBadge },
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
    @PreviewParameter(ReadMessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        ReadMessageItem(
            state = MessageItemUi(
                state = MessageItemUi.State.Read,
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
                encrypted = params.trailingElements.any { it == MessageItemTrailingElement.EncryptedBadge },
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

package com.fsck.k9.ui.messagelist.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import app.k9mail.core.android.common.contact.ContactRepository
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import kotlin.time.ExperimentalTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemAccountIndicator
import net.thunderbird.feature.mail.message.list.ui.component.organism.ReadMessageItem
import net.thunderbird.feature.mail.message.list.ui.component.organism.UnreadMessageItem
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Avatar
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressStyle
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalTime::class)
@Composable
@Deprecated("Don't use. Will be removed soon.")
internal fun MessageItemContent(
    item: MessageListItem,
    isActive: Boolean,
    isSelected: Boolean,
    contactRepository: ContactRepository,
    avatarMonogramCreator: AvatarMonogramCreator,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onFavouriteClick: (Boolean) -> Unit,
    appearance: MessageListAppearance,
) {
    val uri by remember(item.displayAddress?.address) {
        mutableStateOf(
            contactRepository.getPhotoUri(
                item.displayAddress?.address ?: "",
            ),
        )
    }
    val monogram by remember(item.displayName.toString(), item.displayAddress?.address) {
        mutableStateOf(avatarMonogramCreator.create(item.displayName.toString(), item.displayAddress?.address))
    }

    val messageItemUi = rememberMessageItemUi(
        item = item,
        showContactPicture = appearance.showContactPicture,
        isSelected = isSelected,
        isActive = isActive,
        monogram = monogram,
        url = uri?.toString(),
    )

    val preferences = remember(appearance) {
        MessageListPreferences(
            density = appearance.density,
            groupConversations = appearance.showingThreadedList,
            showCorrespondentNames = false,
            showMessageAvatar = appearance.showContactPicture,
            showFavouriteButton = appearance.stars,
            senderAboveSubject = appearance.senderAboveSubject,
            excerptLines = appearance.previewLines,
            dateTimeFormat = appearance.dateTimeFormat,
            colorizeBackgroundWhenRead = appearance.backGroundAsReadIndicator,
        )
    }
    val accountIndicator = remember(appearance) {
        if (appearance.showAccountIndicator) {
            MessageItemAccountIndicator(color = Color(item.account.profile.color))
        } else {
            null
        }
    }
    when {
        item.isRead -> ReadMessageItem(
            state = messageItemUi,
            preferences = preferences,
            accountIndicator = accountIndicator,
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteChange = onFavouriteClick,
        )

        else -> UnreadMessageItem(
            state = messageItemUi,
            preferences = preferences,
            accountIndicator = accountIndicator,
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteChange = onFavouriteClick,
        )
    }
}

@Composable
private fun rememberMessageItemUi(
    item: MessageListItem,
    showContactPicture: Boolean,
    isSelected: Boolean,
    isActive: Boolean,
    monogram: String,
    url: String?,
): MessageItemUi = remember(item, showContactPicture, isSelected, isActive, monogram, url) {
    MessageItemUi(
        state = if (item.isRead) MessageItemUi.State.Read else MessageItemUi.State.Unread,
        id = item.messageUid,
        account = Account(
            id = item.account.id,
            color = Color(item.account.profile.color),
        ),
        senders = ComposedAddressUi(
            displayName = item.displayAddress?.address ?: "",
            displayNameStyles = item.buildSenderStyles(),
            avatar = when {
                !showContactPicture -> null
                showContactPicture && url != null -> Avatar.Image(url = url)
                else -> Avatar.Monogram(monogram)
            },
            color = Color(item.contactColor),
        ),
        subject = item.subject ?: "n/a",
        excerpt = item.previewText,
        formattedReceivedAt = item.displayMessageDateTime,
        hasAttachments = item.hasAttachments,
        starred = item.isStarred,
        encrypted = item.isMessageEncrypted,
        answered = item.isAnswered,
        forwarded = item.isForwarded,
        selected = isSelected,
        threadCount = item.threadCount,
        active = isActive,
    )
}

private fun MessageListItem.buildSenderStyles(): ImmutableList<ComposedAddressStyle> = buildList {
    when (val separatorIndex = displayName.indexOf(',')) {
        -1 if !isRead -> add(ComposedAddressStyle.Bold(start = 0))
        in 0..Int.MAX_VALUE if !isRead -> {
            add(ComposedAddressStyle.Bold(start = 0, end = separatorIndex))
            add(ComposedAddressStyle.Regular(start = separatorIndex))
        }

        else -> add(ComposedAddressStyle.Regular(start = 0))
    }
}.toPersistentList()

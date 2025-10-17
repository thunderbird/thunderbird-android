package com.fsck.k9.ui.messagelist.item

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.image.RemoteImage
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.core.ui.compose.designsystem.organism.message.ActiveMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.ReadMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.UnreadMessageItem
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalTime::class)
@Composable
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
    val receivedAt = remember(item.messageDate) {
        Instant.fromEpochMilliseconds(item.messageDate)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }

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

    when {
        isActive -> ActiveMessageItem(
            sender = "${item.displayName}",
            subject = item.subject ?: "n/a",
            preview = item.previewText,
            receivedAt = receivedAt,
            avatar = {
                if (appearance.showContactPicture) {
                    ContactImageAvatar(
                        contactImageUri = uri,
                        contactImageMonogram = monogram,
                        onAvatarClick = onAvatarClick,
                    )
                }
            },
            onClick = onClick,
            onLongClick = onLongClick,
            onLeadingClick = onAvatarClick,
            onFavouriteChange = onFavouriteClick,
            favourite = item.isStarred,
            selected = isSelected,
            maxPreviewLines = appearance.previewLines,
            threadCount = item.threadCount,
            hasAttachments = item.hasAttachments,
            swapSenderWithSubject = !appearance.senderAboveSubject,
        )

        item.isRead -> ReadMessageItem(
            sender = "${item.displayName}",
            subject = item.subject ?: "n/a",
            preview = item.previewText,
            receivedAt = receivedAt,
            avatar = {
                if (appearance.showContactPicture) {
                    ContactImageAvatar(
                        contactImageUri = uri,
                        contactImageMonogram = monogram,
                        onAvatarClick = onAvatarClick,
                    )
                }
            },
            onClick = onClick,
            onLongClick = onLongClick,
            onLeadingClick = onAvatarClick,
            onFavouriteChange = onFavouriteClick,
            favourite = item.isStarred,
            selected = isSelected,
            maxPreviewLines = appearance.previewLines,
            threadCount = item.threadCount,
            hasAttachments = item.hasAttachments,
            swapSenderWithSubject = !appearance.senderAboveSubject,
        )

        else -> UnreadMessageItem(
            sender = "${item.displayName}",
            subject = item.subject ?: "n/a",
            preview = item.previewText,
            receivedAt = receivedAt,
            avatar = {
                if (appearance.showContactPicture) {
                    ContactImageAvatar(
                        contactImageUri = uri,
                        contactImageMonogram = monogram,
                        onAvatarClick = onAvatarClick,
                    )
                }
            },
            onClick = onClick,
            onLongClick = onLongClick,
            onLeadingClick = onAvatarClick,
            onFavouriteChange = onFavouriteClick,
            favourite = item.isStarred,
            selected = isSelected,
            maxPreviewLines = appearance.previewLines,
            threadCount = item.threadCount,
            hasAttachments = item.hasAttachments,
            swapSenderWithSubject = !appearance.senderAboveSubject,
        )
    }
}

@Composable
fun ContactImageAvatar(
    contactImageUri: Uri?,
    contactImageMonogram: String,
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MainTheme.sizes.iconAvatar)
            .padding(MainTheme.spacings.half)
            .background(color = MainTheme.colors.primaryContainer.copy(alpha = 0.15f), shape = CircleShape)
            .border(width = 1.dp, color = MainTheme.colors.primary, shape = CircleShape)
            .clickable(onClick = onAvatarClick),
    ) {
        contactImageUri?.let {
            RemoteImage(
                url = it.toString(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                placeholder = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(MainTheme.sizes.iconAvatar)) {
                        CircularProgressIndicator(modifier = Modifier.size(MainTheme.sizes.icon))
                    }
                },
            )
        } ?: run {
            TextTitleSmall(text = contactImageMonogram)
        }
    }
}

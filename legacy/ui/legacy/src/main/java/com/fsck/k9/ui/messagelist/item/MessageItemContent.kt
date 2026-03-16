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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.image.RemoteImage
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import kotlin.time.ExperimentalTime
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.mail.message.list.ui.component.organism.MessageItemDefaults
import net.thunderbird.feature.mail.message.list.ui.component.organism.ReadMessageItem
import net.thunderbird.feature.mail.message.list.ui.component.organism.UnreadMessageItem

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
    val receivedAt = item.displayMessageDateTime

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
    val contentPadding = when (appearance.density) {
        UiDensity.Compact -> MessageItemDefaults.compactContentPadding
        UiDensity.Default -> MessageItemDefaults.defaultContentPadding
        UiDensity.Relaxed -> MessageItemDefaults.relaxedContentPadding
    }

    when {
        item.isRead -> ReadMessageItem(
            sender = buildAnnotatedString { append("${item.displayName}") },
            subject = item.subject ?: "n/a",
            preview = item.previewText,
            receivedAt = receivedAt,
            showAccountIndicator = appearance.showAccountIndicator,
            accountIndicatorColor = Color(item.account.profile.color),
            avatar = {
                if (appearance.showContactPicture) {
                    ContactImageAvatar(
                        color = Color(item.contactColor),
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
            contentPadding = contentPadding,
        )

        else -> UnreadMessageItem(
            sender = buildAnnotatedString { append("${item.displayName}") },
            subject = item.subject ?: "n/a",
            preview = item.previewText,
            receivedAt = receivedAt,
            showAccountIndicator = appearance.showAccountIndicator,
            accountIndicatorColor = Color(item.account.profile.color),
            avatar = {
                if (appearance.showContactPicture) {
                    ContactImageAvatar(
                        color = Color(item.contactColor),
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
            contentPadding = contentPadding,
        )
    }
}

@Composable
fun ContactImageAvatar(
    color: Color,
    contactImageUri: Uri?,
    contactImageMonogram: String,
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MainTheme.sizes.iconAvatar)
            .padding(MainTheme.spacings.half)
            .background(color = color.copy(alpha = 0.15f), shape = CircleShape)
            .border(width = 1.dp, color = color, shape = CircleShape)
            .clickable(onClick = onAvatarClick),
    ) {
        contactImageUri?.let {
            RemoteImage(
                url = it.toString(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
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

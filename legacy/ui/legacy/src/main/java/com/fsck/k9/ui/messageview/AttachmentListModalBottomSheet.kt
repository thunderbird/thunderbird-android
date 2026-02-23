package com.fsck.k9.ui.messageview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.ui.R
import com.fsck.k9.ui.helper.SizeFormatter
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.organism.ModalBottomSheet

private const val OPEN_PGP_RED = 0xFFCC0000

internal data class AttachmentListItemModel(
    val attachment: AttachmentViewInfo,
    val isLocked: Boolean,
)

@Composable
internal fun AttachmentListModalBottomSheet(
    attachments: ImmutableList<AttachmentListItemModel>,
    sizeFormatter: SizeFormatter,
    onDismissRequest: () -> Unit,
    onAttachmentClick: (AttachmentViewInfo) -> Unit,
    onSaveClick: (AttachmentViewInfo) -> Unit,
    onSaveAllClick: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        AttachmentListContent(
            attachments = attachments,
            sizeFormatter = sizeFormatter,
            onAttachmentClick = onAttachmentClick,
            onSaveClick = onSaveClick,
            onSaveAllClick = onSaveAllClick,
        )
    }
}

@Composable
private fun AttachmentListContent(
    attachments: ImmutableList<AttachmentListItemModel>,
    sizeFormatter: SizeFormatter,
    onAttachmentClick: (AttachmentViewInfo) -> Unit,
    onSaveClick: (AttachmentViewInfo) -> Unit,
    onSaveAllClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        AttachmentListHeader(onSaveAllClick = onSaveAllClick)

        attachments.forEach { item ->
            AttachmentListItem(
                attachment = item.attachment,
                isLocked = item.isLocked,
                sizeFormatter = sizeFormatter,
                onClick = { onAttachmentClick(item.attachment) },
                onSaveClick = { onSaveClick(item.attachment) },
            )
        }
    }
}

@Composable
private fun AttachmentListHeader(
    onSaveAllClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MainTheme.spacings.double,
                end = MainTheme.spacings.double,
                bottom = MainTheme.spacings.default,
                top = MainTheme.spacings.double,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Attachment,
            contentDescription = null,
            tint = MainTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(MainTheme.sizes.icon),
        )
        Spacer(modifier = Modifier.width(MainTheme.spacings.default))
        TextTitleMedium(
            text = stringResource(R.string.message_view_attachments_title),
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.clickable(onClick = onSaveAllClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                tint = MainTheme.colors.primary,
                modifier = Modifier.size(MainTheme.sizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(MainTheme.spacings.half))
            TextLabelLarge(
                text = stringResource(R.string.message_view_attachments_save_all),
                color = MainTheme.colors.primary,
            )
        }
    }
}

@Composable
private fun AttachmentListItem(
    attachment: AttachmentViewInfo,
    isLocked: Boolean,
    sizeFormatter: SizeFormatter,
    onClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = MainTheme.spacings.double,
                end = MainTheme.spacings.default,
                top = MainTheme.spacings.default,
                bottom = MainTheme.spacings.default,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isLocked) Icons.Outlined.Lock else Icons.Outlined.Description,
            contentDescription = null,
            tint = if (isLocked) Color(OPEN_PGP_RED) else MainTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(MainTheme.sizes.icon),
        )
        Spacer(modifier = Modifier.width(MainTheme.spacings.double))
        Column(modifier = Modifier.weight(1f)) {
            TextBodyMedium(
                text = if (isLocked) stringResource(R.string.encrypted_attachment_title) else attachment.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isLocked) {
                TextBodySmall(
                    text = stringResource(R.string.locked_attach_unencrypted),
                    color = MainTheme.colors.onSurfaceVariant,
                )
            } else if (attachment.size != AttachmentViewInfo.UNKNOWN_SIZE) {
                TextBodySmall(
                    text = sizeFormatter.formatSize(attachment.size),
                    color = MainTheme.colors.onSurfaceVariant,
                )
            }
        }
        if (isLocked) {
            ButtonIcon(
                onClick = onClick,
                imageVector = Icons.Outlined.Visibility,
                contentDescription = stringResource(R.string.locked_attach_unlock),
            )
        }
        ButtonIcon(
            onClick = onSaveClick,
            imageVector = Icons.Outlined.Download,
            contentDescription = stringResource(R.string.save_attachment_action),
        )
    }
}

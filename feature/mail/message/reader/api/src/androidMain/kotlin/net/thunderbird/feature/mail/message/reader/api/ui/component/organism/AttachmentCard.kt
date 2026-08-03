package net.thunderbird.feature.mail.message.reader.api.ui.component.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.components.ui.bolt.atom.CircularProgressIndicator
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.card.CardOutlined
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.image.RemoteImage
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.message.reader.api.R
import net.thunderbird.feature.mail.message.reader.api.ui.attachment.AttachmentUiItem

@Composable
fun <TPart> AttachmentCard(
    attachment: AttachmentUiItem<TPart>,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardOutlined(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column {
            AttachmentThumbnail(attachment)

            Surface(color = BoltTheme.colors.surfaceContainerHigh) {
                Row(
                    modifier = Modifier
                        .padding(
                            start = BoltTheme.spacings.double,
                            top = BoltTheme.spacings.default,
                            end = BoltTheme.spacings.default,
                            bottom = BoltTheme.spacings.default,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.quadruple),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when (attachment) {
                            is AttachmentUiItem.File, is AttachmentUiItem.InlinedFile -> Icons.Outlined.Description
                            is AttachmentUiItem.InlinedImage, is AttachmentUiItem.RemoteImage -> Icons.Outlined.Image
                        },
                        tint = BoltTheme.colors.primary,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        TextBodyMedium(
                            text = when (val filename = attachment.filename) {
                                null -> stringResource(R.string.unnamed_attachment_title)
                                else if attachment.encrypted -> stringResource(R.string.encrypted_attachment_title)
                                else -> filename
                            },
                        )
                        TextBodySmall(text = attachment.formattedSize, color = BoltTheme.colors.primary)
                    }
                    ButtonIcon(
                        onClick = onDownloadClick,
                        imageVector = if (attachment.encrypted) {
                            Icons.Outlined.Lock
                        } else {
                            Icons.Outlined.Download
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun <TPart> AttachmentThumbnail(attachment: AttachmentUiItem<TPart>, modifier: Modifier = Modifier) {
    val thumbnailModifier = modifier
        .fillMaxWidth()
        .height(BoltTheme.sizes.huge)
    when (attachment) {
        is AttachmentUiItem.RemoteImage -> RemoteImage(
            url = attachment.url,
            placeholder = { AttachmentThumbnailPlaceholder() },
            modifier = thumbnailModifier,
        )

        is AttachmentUiItem.File -> Unit

        is AttachmentUiItem.InlinedFile -> Unit

        is AttachmentUiItem.InlinedImage -> {
            val dataUri = remember(attachment.rawBase64) {
                if (attachment.rawBase64.startsWith("data:", ignoreCase = true)) {
                    attachment.rawBase64
                } else {
                    "data:image/png;base64,$attachment.rawBase64"
                }
            }
            RemoteImage(
                url = dataUri,
                placeholder = { AttachmentThumbnailPlaceholder() },
                modifier = thumbnailModifier,
            )
        }
    }
}

@Composable
private fun BoxScope.AttachmentThumbnailPlaceholder() {
    Box(modifier = Modifier.fillMaxSize().align(Alignment.Center)) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(BoltTheme.sizes.large)
                .align(Alignment.Center),
        )
    }
}

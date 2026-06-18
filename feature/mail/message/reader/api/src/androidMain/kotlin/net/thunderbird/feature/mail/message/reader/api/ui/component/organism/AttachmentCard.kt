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
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.card.CardOutlined
import app.k9mail.core.ui.compose.designsystem.atom.image.RemoteImage
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
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

            Surface(color = MainTheme.colors.surfaceContainerHigh) {
                Row(
                    modifier = Modifier
                        .padding(
                            start = MainTheme.spacings.double,
                            top = MainTheme.spacings.default,
                            end = MainTheme.spacings.default,
                            bottom = MainTheme.spacings.default,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.quadruple),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when (attachment) {
                            is AttachmentUiItem.File, is AttachmentUiItem.InlinedFile -> Icons.Outlined.Description
                            is AttachmentUiItem.InlinedImage, is AttachmentUiItem.RemoteImage -> Icons.Outlined.Image
                        },
                        tint = MainTheme.colors.primary,
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
                        TextBodySmall(text = attachment.formattedSize, color = MainTheme.colors.primary)
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
        .height(MainTheme.sizes.huge)
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
                .size(MainTheme.sizes.large)
                .align(Alignment.Center),
        )
    }
}

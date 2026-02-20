package com.fsck.k9.ui.messageview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.ui.R
import com.fsck.k9.ui.helper.SizeFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons as DesignIcons
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject

class AttachmentListBottomSheet : BottomSheetDialogFragment() {

    private val themeProvider: FeatureThemeProvider by inject()

    private var attachments: List<AttachmentViewInfo> = emptyList()
    private var attachmentCallback: AttachmentViewCallback? = null

    fun setData(attachments: List<AttachmentViewInfo>, callback: AttachmentViewCallback) {
        this.attachments = attachments
        this.attachmentCallback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (attachments.isEmpty()) {
            dismissAllowingStateLoss()
        }

        val sizeFormatter = SizeFormatter(resources)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme {
                    AttachmentListContent(
                        attachments = attachments,
                        sizeFormatter = sizeFormatter,
                        onAttachmentClick = { attachmentCallback?.onViewAttachment(it) },
                        onSaveClick = { attachmentCallback?.onSaveAttachment(it) },
                        onSaveAllClick = {
                            attachments.forEach { attachmentCallback?.onSaveAttachment(it) }
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "AttachmentListBottomSheet"

        fun newInstance(): AttachmentListBottomSheet {
            return AttachmentListBottomSheet()
        }
    }
}

@Composable
private fun AttachmentListContent(
    attachments: List<AttachmentViewInfo>,
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

        attachments.forEach { attachment ->
            AttachmentListItem(
                attachment = attachment,
                sizeFormatter = sizeFormatter,
                onClick = { onAttachmentClick(attachment) },
                onSaveClick = { onSaveClick(attachment) },
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
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = DesignIcons.Outlined.Attachment,
            contentDescription = null,
            tint = MainTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
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
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
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
    sizeFormatter: SizeFormatter,
    onClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MainTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            TextBodyMedium(
                text = attachment.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (attachment.size != AttachmentViewInfo.UNKNOWN_SIZE) {
                TextBodySmall(
                    text = sizeFormatter.formatSize(attachment.size),
                    color = MainTheme.colors.onSurfaceVariant,
                )
            }
        }
        ButtonIcon(
            onClick = onSaveClick,
            imageVector = Icons.Outlined.Download,
            contentDescription = stringResource(R.string.save_attachment_action),
        )
    }
}

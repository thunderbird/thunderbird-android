package com.fsck.k9.ui.messageview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import app.k9mail.core.android.common.activity.findActivity
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.ui.R
import com.fsck.k9.ui.helper.SizeFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textview.MaterialTextView

class AttachmentListBottomSheet : BottomSheetDialogFragment() {

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
        return inflater.inflate(R.layout.bottom_sheet_attachment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (attachments.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }

        val listContainer = view.findViewById<LinearLayout>(R.id.attachment_list_container)
        val saveAllButton = view.findViewById<View>(R.id.save_all_button)
        val sizeFormatter = SizeFormatter(resources)
        val inflater = LayoutInflater.from(requireContext())

        saveAllButton.setOnClickListener {
            for (attachment in attachments) {
                attachmentCallback?.onSaveAttachment(attachment)
            }
        }

        for (attachment in attachments) {
            val itemView = inflater.inflate(R.layout.attachment_list_item, listContainer, false)
            val icon = itemView.findViewById<ImageView>(R.id.attachment_icon)
            val nameView = itemView.findViewById<MaterialTextView>(R.id.attachment_name)
            val sizeView = itemView.findViewById<MaterialTextView>(R.id.attachment_size)
            val saveButton = itemView.findViewById<ImageButton>(R.id.save_button)

            nameView.text = attachment.displayName

            if (attachment.size == AttachmentViewInfo.UNKNOWN_SIZE) {
                sizeView.text = ""
            } else {
                sizeView.text = sizeFormatter.formatSize(attachment.size)
            }

            if (attachment.isSupportedImage() && attachment.isContentAvailable()) {
                icon.clearColorFilter()
                icon.scaleType = ImageView.ScaleType.CENTER_CROP
                loadThumbnail(icon, attachment)
            } else {
                icon.setImageResource(Icons.Outlined.Description)
            }

            itemView.setOnClickListener {
                attachmentCallback?.onViewAttachment(attachment)
            }

            saveButton.setOnClickListener {
                attachmentCallback?.onSaveAttachment(attachment)
            }

            listContainer.addView(itemView)
        }
    }

    private fun loadThumbnail(imageView: ImageView, attachment: AttachmentViewInfo) {
        val context = imageView.context
        val activity = context.findActivity()
        if (activity != null && activity.isDestroyed) {
            return
        }

        Glide.with(context)
            .load(attachment.internalUri)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(imageView)
    }

    companion object {
        const val TAG = "AttachmentListBottomSheet"

        fun newInstance(): AttachmentListBottomSheet {
            return AttachmentListBottomSheet()
        }
    }
}

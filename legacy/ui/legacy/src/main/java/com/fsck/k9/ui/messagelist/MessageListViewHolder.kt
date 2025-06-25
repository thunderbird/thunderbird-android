package com.fsck.k9.ui.messagelist

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView

sealed class MessageListViewHolder(view: View) : ViewHolder(view)

class MessageViewHolder(view: View) : MessageListViewHolder(view) {
    var uniqueId: Long = -1L

    val selected: View = view.findViewById(R.id.selected)
    val contactPicture: ImageView = view.findViewById(R.id.contact_picture)
    val contactPictureClickArea: View = view.findViewById(R.id.contact_picture_click_area)
    val subject: MaterialTextView = view.findViewById(R.id.subject)
    val preview: MaterialTextView = view.findViewById(R.id.preview)
    val date: MaterialTextView = view.findViewById(R.id.date)
    val chip: ImageView = view.findViewById(R.id.account_color_chip)
    val threadCount: MaterialTextView = view.findViewById(R.id.thread_count)
    val star: ImageView = view.findViewById(R.id.star)
    val starClickArea: View = view.findViewById(R.id.star_click_area)
    val attachment: ImageView = view.findViewById(R.id.attachment)
    val status: ImageView = view.findViewById(R.id.status)
}

class FooterViewHolder(view: View) : MessageListViewHolder(view) {
    val text: MaterialTextView = view.findViewById(R.id.main_text)
}

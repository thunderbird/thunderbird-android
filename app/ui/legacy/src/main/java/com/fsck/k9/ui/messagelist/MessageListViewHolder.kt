package com.fsck.k9.ui.messagelist

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.fsck.k9.ui.R

sealed class MessageListViewHolder(view: View) : ViewHolder(view)

class MessageViewHolder(view: View) : MessageListViewHolder(view) {
    var uniqueId: Long = -1L

    val selected: View = view.findViewById(R.id.selected)
    val contactPicture: ImageView = view.findViewById(R.id.contact_picture)
    val subject: TextView = view.findViewById(R.id.subject)
    val preview: TextView = view.findViewById(R.id.preview)
    val date: TextView = view.findViewById(R.id.date)
    val chip: ImageView = view.findViewById(R.id.account_color_chip)
    val threadCount: TextView = view.findViewById(R.id.thread_count)
    val star: ImageView = view.findViewById(R.id.star)
    val starClickArea: View = view.findViewById(R.id.star_click_area)
    val attachment: ImageView = view.findViewById(R.id.attachment)
    val status: ImageView = view.findViewById(R.id.status)
}

class FooterViewHolder(view: View) : MessageListViewHolder(view) {
    val text: TextView = view.findViewById(R.id.main_text)
}

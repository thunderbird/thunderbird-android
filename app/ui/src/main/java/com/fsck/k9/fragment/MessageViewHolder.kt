package com.fsck.k9.fragment


import android.annotation.SuppressLint
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.fsck.k9.ui.ContactBadge
import com.fsck.k9.ui.R

@SuppressLint("ResourceType")
class MessageViewHolder(
        view: View,
        private val itemActionListener: MessageListItemActionListener
) : View.OnClickListener {
    var position = -1

    val contactBadge: ContactBadge = view.findViewById(R.id.contact_badge)
    val subject: TextView = view.findViewById(R.id.subject)
    val preview: TextView = view.findViewById(R.id.preview)
    val date: TextView = view.findViewById(R.id.date)
    val chip: ImageView = view.findViewById(R.id.account_color_chip)
    val threadCount: TextView = view.findViewById(R.id.thread_count)
    val flagged: CheckBox = view.findViewById(R.id.star)
    val attachment: ImageView = view.findViewById(R.id.attachment)
    val status: ImageView = view.findViewById(R.id.status)

    override fun onClick(view: View) {
        if (position != -1) {
            if (view.id == R.id.star) {
                itemActionListener.toggleMessageFlagWithAdapterPosition(position)
            }
        }
    }
}

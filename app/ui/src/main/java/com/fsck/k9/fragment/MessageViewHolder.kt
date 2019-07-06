package com.fsck.k9.fragment


import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.fsck.k9.FontSizes
import com.fsck.k9.ui.ContactBadge
import com.fsck.k9.ui.R
import kotlinx.android.synthetic.main.message_list_item.view.*

class MessageViewHolder(
        private val view: View,
        private val compact: Boolean,
        private val senderAboveSubject: Boolean,
        private val toggleMessageSelectWithAdapterPosition: (Int) -> Unit,
        private val toggleMessageFlagWithAdapterPosition: (Int) -> Unit,
        private val fontSizes: FontSizes
) : View.OnClickListener {

    init {
        if (compact) {
            view.preview.visibility = View.GONE
            view.flagged_bottom_right.visibility = View.GONE
        } else {
            view.sender_compact.visibility = View.GONE
            view.flagged_center_right.visibility = View.GONE
        }
        if (senderAboveSubject) {
            fontSizes.setViewTextSize(from, fontSizes.messageListSender)
        } else {
            fontSizes.setViewTextSize(subject, fontSizes.messageListSubject)
        }

    }

    val subject: TextView? get() = if (senderAboveSubject) null else view.subject
    val from: TextView? get() = if (senderAboveSubject) view.subject else null
    val preview: TextView get() = if (compact) view.sender_compact else view.preview
    val flagged: CheckBox get() = if (compact) view.flagged_center_right else view.flagged_bottom_right
    val date: TextView get() = view.date
    val chip: View get() = view.chip
    val threadCount: TextView get() = view.thread_count
    val selected: CheckBox get() = view.selected_checkbox
    val contactBadge: ContactBadge get() = view.contact_badge
    val attachment: ImageView get() = view.attachment
    val status: ImageView get() = view.status

    var position = -1

    override fun onClick(view: View) {
        if (position != -1) {
            val id = view.id
            if (id == R.id.selected_checkbox) {
                toggleMessageSelectWithAdapterPosition(position)
            } else if (id == R.id.flagged_bottom_right || id == R.id.flagged_center_right) {
                toggleMessageFlagWithAdapterPosition(position)
            }
        }
    }
}

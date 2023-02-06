package com.fsck.k9.ui.messagedetails

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

internal class ParticipantItem(
    private val contactPictureLoader: ContactPictureLoader,
    private val showContactsPicture: Boolean,
    val participant: Participant
) : AbstractItem<ParticipantItem.ViewHolder>() {
    override val type: Int = R.id.message_details_participant
    override val layoutRes = R.layout.message_details_participant_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ParticipantItem>(view) {
        val menuAddContact: View = view.findViewById(R.id.menu_add_contact)
        val menuCompose: View = view.findViewById(R.id.menu_compose)
        val menuOverflow: View = view.findViewById(R.id.menu_overflow)

        private val contactPicture: ImageView = view.findViewById(R.id.contact_picture)
        private val name = view.findViewById<TextView>(R.id.name)
        private val email = view.findViewById<TextView>(R.id.email)
        private val originalBackground = view.background

        init {
            TooltipCompat.setTooltipText(menuAddContact, menuAddContact.contentDescription)
            TooltipCompat.setTooltipText(menuCompose, menuCompose.contentDescription)
            TooltipCompat.setTooltipText(menuOverflow, menuOverflow.contentDescription)
        }

        override fun bindView(item: ParticipantItem, payloads: List<Any>) {
            val participant = item.participant

            if (participant.displayName != null) {
                name.text = participant.displayName
                email.text = participant.emailAddress
            } else {
                name.text = participant.emailAddress
                email.isVisible = false
            }
            menuAddContact.isVisible = !participant.isInContacts

            if (item.showContactsPicture) {
                item.contactPictureLoader.setContactPicture(contactPicture, participant.address)
            } else {
                contactPicture.isVisible = false
            }

            if (!item.participant.isInContacts) {
                itemView.isClickable = false
                itemView.background = null
            }
        }

        override fun unbindView(item: ParticipantItem) {
            name.text = null
            email.text = null
            email.isVisible = true
            contactPicture.isVisible = true
            itemView.background = originalBackground
            itemView.isClickable = true
        }
    }
}

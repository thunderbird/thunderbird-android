package com.fsck.k9.ui.settings.openpgp

import com.fsck.k9.Identity
import com.fsck.k9.ui.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.settings_openpgp_identity_list_item.*
import kotlinx.android.synthetic.main.settings_openpgp_identity_list_item.view.*

internal class IdentityItem(val identity: Identity, val identityListener: OnIdentityClickedListener) : Item() {
    interface OnIdentityClickedListener {
        fun onIdentityClicked(item: IdentityItem, identity: Identity)
        fun onCheckedChange(item: IdentityItem, identity: Identity, checked: Boolean)
    }

    override fun getLayout(): Int = R.layout.settings_openpgp_identity_list_item

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.identityEmail.text = identity.email
        viewHolder.identityDescription.text = "todo"

        viewHolder.itemView.identityItem.setOnClickListener {
            identityListener.onIdentityClicked(this@IdentityItem, identity)
        }
        viewHolder.itemView.identitySwitch.setOnCheckedChangeListener { _, isChecked ->
            identityListener.onCheckedChange(this@IdentityItem, identity, isChecked)
        }
    }
}

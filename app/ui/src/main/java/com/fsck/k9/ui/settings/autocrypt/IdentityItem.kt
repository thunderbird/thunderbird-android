package com.fsck.k9.ui.settings.autocrypt

import com.fsck.k9.Identity
import com.fsck.k9.ui.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.account_list_item.*
import kotlinx.android.synthetic.main.settings_autocrypt_identity_list_item.*

internal class IdentityItem(val identity: Identity) : Item() {

    override fun getLayout(): Int = R.layout.settings_autocrypt_identity_list_item

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.autocryptIdentityEmail.text = identity.email
        viewHolder.autocryptIdentityDescription.text = "todo"
    }
}

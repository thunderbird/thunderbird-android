package com.fsck.k9.ui.settings

import com.fsck.k9.Account
import com.fsck.k9.ui.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.account_list_item.*

internal class AccountItem(val account: Account) : Item() {

    override fun getLayout(): Int = R.layout.account_list_item

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.name.text = account.description
        viewHolder.email.text = account.email
    }
}

package com.fsck.k9.ui.settings

import android.view.View
import android.widget.TextView
import com.fsck.k9.Account
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

internal class AccountItem(val account: Account) : AbstractItem<AccountItem.ViewHolder>() {
    override var identifier = 200L + account.accountNumber

    override val type = R.id.settings_list_account_item

    override val layoutRes = R.layout.account_list_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<AccountItem>(view) {
        val name: TextView = view.findViewById(R.id.name)
        val email: TextView = view.findViewById(R.id.email)

        override fun bindView(item: AccountItem, payloads: List<Any>) {
            name.text = item.account.description
            email.text = item.account.email
        }

        override fun unbindView(item: AccountItem) {
            name.text = null
            email.text = null
        }
    }
}

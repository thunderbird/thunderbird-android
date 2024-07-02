package com.fsck.k9.ui.settings.export

import android.view.View
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView

private const val GENERAL_SETTINGS_ID = 0L
private const val ACCOUNT_ITEMS_ID_OFFSET = 1L

class GeneralSettingsItem : CheckBoxItem<CheckBoxViewHolder>(GENERAL_SETTINGS_ID) {
    override val type = R.id.settings_export_list_general_item
    override val layoutRes = R.layout.settings_export_general_list_item

    override fun getViewHolder(v: View) = CheckBoxViewHolder(v)
}

class AccountViewHolder(view: View) : CheckBoxViewHolder(view) {
    val accountDisplayName: MaterialTextView = view.findViewById(R.id.accountDisplayName)
    val accountEmail: MaterialTextView = view.findViewById(R.id.accountEmail)
}

class AccountItem(account: SettingsListItem.Account) :
    CheckBoxItem<AccountViewHolder>(account.accountNumber + ACCOUNT_ITEMS_ID_OFFSET) {

    private val displayName = account.displayName
    private val email = account.email

    override val type = R.id.settings_export_list_account_item
    override val layoutRes = R.layout.settings_export_account_list_item

    override fun getViewHolder(v: View) = AccountViewHolder(v)

    override fun bindView(holder: AccountViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.accountDisplayName.text = displayName
        holder.accountEmail.text = email
    }
}

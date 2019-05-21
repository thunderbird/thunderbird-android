package com.fsck.k9.ui.settings.export

import com.fsck.k9.ui.R
import kotlinx.android.synthetic.main.settings_export_account_list_item.*


private const val GENERAL_SETTINGS_ID = 0L
private const val ACCOUNT_ITEMS_ID_OFFSET = 1L


class GeneralSettingsItem : CheckBoxItem(GENERAL_SETTINGS_ID) {
    override fun getType(): Int = R.id.settings_export_list_general_item

    override fun getLayoutRes(): Int = R.layout.settings_export_general_list_item
}


class AccountItem(account: SettingsListItem.Account) : CheckBoxItem(account.accountNumber + ACCOUNT_ITEMS_ID_OFFSET) {
    private val displayName = account.displayName
    private val email = account.email


    override fun getType(): Int = R.id.settings_export_list_account_item

    override fun getLayoutRes(): Int = R.layout.settings_export_account_list_item

    override fun bindView(viewHolder: CheckBoxViewHolder, payloads: List<Any>) {
        super.bindView(viewHolder, payloads)
        viewHolder.accountDisplayName.text = displayName
        viewHolder.accountEmail.text = email
    }
}

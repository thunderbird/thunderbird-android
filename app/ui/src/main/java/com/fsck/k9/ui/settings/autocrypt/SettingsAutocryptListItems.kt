package com.fsck.k9.ui.settings.autocrypt

import com.fsck.k9.ui.R
import kotlinx.android.synthetic.main.settings_autocrypt_identity_list_item.*


private const val GENERAL_SETTINGS_ID = 0L
private const val ACCOUNT_ITEMS_ID_OFFSET = 1L


class AdvancedSettingsItem : SwitchItem(GENERAL_SETTINGS_ID) {
    override fun getType(): Int = R.id.settings_export_list_general_item

    override fun getLayoutRes(): Int = R.layout.settings_autocrypt_identity_list_item
}

class AutocryptIdentityItem(account: SettingsListItem.AutocryptIdentity) : SwitchItem(account.accountNumber + ACCOUNT_ITEMS_ID_OFFSET) {
    private val email = account.email


    override fun getType(): Int = R.id.settings_encryption_list_identity_item

    override fun getLayoutRes(): Int = R.layout.settings_autocrypt_identity_list_item

    override fun bindView(viewHolder: SwitchViewHolder, payloads: List<Any>) {
        super.bindView(viewHolder, payloads)
        viewHolder.autocryptIdentityEmail.text = email
        viewHolder.autocryptIdentityDescription.text = "todo"
    }
}

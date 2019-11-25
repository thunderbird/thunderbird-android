package com.fsck.k9.ui.settings.encryption

import com.fsck.k9.ui.R
import kotlinx.android.synthetic.main.settings_encryption_identity_list_item.*
import kotlinx.android.synthetic.main.settings_export_account_list_item.*


private const val GENERAL_SETTINGS_ID = 0L
private const val ACCOUNT_ITEMS_ID_OFFSET = 1L


class AdvancedSettingsItem : EncryptionSwitchItem(GENERAL_SETTINGS_ID) {
    override fun getType(): Int = R.id.settings_export_list_general_item

    override fun getLayoutRes(): Int = R.layout.settings_encryption_identity_list_item
}

class EncryptionIdentityItem(account: SettingsListItem.EncryptionIdentity) : EncryptionSwitchItem(account.accountNumber + ACCOUNT_ITEMS_ID_OFFSET) {
    private val displayName = account.displayName
    private val email = account.email


    override fun getType(): Int = R.id.settings_encryption_list_identity_item

    override fun getLayoutRes(): Int = R.layout.settings_encryption_identity_list_item

    override fun bindView(viewHolder: SwitchViewHolder, payloads: List<Any>) {
        super.bindView(viewHolder, payloads)
        viewHolder.encryptionIdentityEmail.text = email
        viewHolder.encryptionIdentityDescription.text = "todo"
    }
}

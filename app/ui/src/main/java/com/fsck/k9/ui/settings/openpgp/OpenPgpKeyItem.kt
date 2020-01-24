package com.fsck.k9.ui.settings.openpgp

import com.fsck.k9.ui.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.openpgp_key_list_item.*
import org.sufficientlysecure.keychain.model.SubKey

class OpenPgpKeyItem(private val keyInfo: SubKey.UnifiedKeyInfo) : Item() {

    override fun getLayout() = R.layout.openpgp_key_list_item

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.keyFingerprintPrefix.text = keyInfo.fingerprint().toString()
    }
}
package com.fsck.k9.ui.helper

import android.content.res.Resources
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.ui.R

class AddressFormatterProvider(
    private val contactNameProvider: ContactNameProvider,
    private val resources: Resources
) {
    fun getAddressFormatter(account: Account): AddressFormatter {
        return RealAddressFormatter(
            contactNameProvider = contactNameProvider,
            account = account,
            showCorrespondentNames = K9.isShowCorrespondentNames,
            showContactNames = K9.isShowContactName,
            contactNameColor = if (K9.isChangeContactNameColor) K9.contactNameColor else null,
            meText = resources.getString(R.string.message_view_me_text)
        )
    }
}

package com.fsck.k9.resources

import android.content.Context
import com.fsck.k9.R
import com.fsck.k9.autocrypt.AutocryptStringProvider

class K9AutocryptStringProvider(private val context: Context) : AutocryptStringProvider {
    override fun transferMessageSubject(): String = context.getString(R.string.ac_transfer_msg_subject)
    override fun transferMessageBody(): String = context.getString(R.string.ac_transfer_msg_body)
}

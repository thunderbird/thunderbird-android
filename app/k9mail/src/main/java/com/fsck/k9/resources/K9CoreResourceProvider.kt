package com.fsck.k9.resources

import android.content.Context
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.R

class K9CoreResourceProvider(private val context: Context) : CoreResourceProvider {
    override fun defaultSignature(): String = context.getString(R.string.default_signature)
    override fun defaultIdentityDescription(): String = context.getString(R.string.default_identity_description)
}

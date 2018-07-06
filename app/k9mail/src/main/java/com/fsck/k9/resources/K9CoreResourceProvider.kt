package com.fsck.k9.resources

import android.content.Context
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.R

class K9CoreResourceProvider(private val context: Context) : CoreResourceProvider {
    override fun defaultSignature(): String = context.getString(R.string.default_signature)
    override fun defaultIdentityDescription(): String = context.getString(R.string.default_identity_description)

    override fun sendAlternateChooserTitle(): String = context.getString(R.string.send_alternate_chooser_title)

    override fun internalStorageProviderName(): String =
            context.getString(R.string.local_storage_provider_internal_label)

    override fun externalStorageProviderName(): String =
            context.getString(R.string.local_storage_provider_external_label)

    override fun contactDisplayNamePrefix(): String = context.getString(R.string.message_to_label)
}

package com.fsck.k9

class TestCoreResourceProvider : CoreResourceProvider {
    override fun defaultSignature(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun defaultIdentityDescription(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun sendAlternateChooserTitle(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun internalStorageProviderName(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun externalStorageProviderName(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun contactDisplayNamePrefix() = "To:"

    override fun messageHeaderFrom() = "From:"
    override fun messageHeaderTo() = "To:"
    override fun messageHeaderCc() = "Cc:"
    override fun messageHeaderDate() = "Sent:"
    override fun messageHeaderSubject() = "Subject:"

    override fun noSubject() = "(No subject)"
}

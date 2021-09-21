package com.fsck.k9.activity.compose

import android.os.Bundle
import com.fsck.k9.Identity
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message

private const val STATE_KEY_REPLY_TO_SHOWN = "com.fsck.k9.activity.compose.ReplyToPresenter.replyToShown"

class ReplyToPresenter(private val view: ReplyToView) {
    private lateinit var identity: Identity
    private var identityReplyTo: Array<Address>? = null

    fun initFromDraftMessage(message: Message) {
        message.replyTo.takeIf { it.isNotEmpty() }?.let { addresses ->
            view.silentlyAddAddresses(addresses)
            view.isVisible = true
        }
    }

    fun getAddresses(): Array<Address> {
        return view.getAddresses()
    }

    fun isNotReadyForSending(): Boolean {
        return if (view.hasUncompletedText()) {
            view.showError()
            view.isVisible = true
            true
        } else {
            false
        }
    }

    fun setIdentity(identity: Identity) {
        this.identity = identity

        removeIdentityReplyTo()
        addIdentityReplyTo()
    }

    private fun addIdentityReplyTo() {
        identityReplyTo = Address.parse(identity.replyTo)?.takeIf { it.isNotEmpty() }
        identityReplyTo?.let { addresses ->
            view.silentlyAddAddresses(addresses)
        }
    }

    private fun removeIdentityReplyTo() {
        identityReplyTo?.let { addresses ->
            view.silentlyRemoveAddresses(addresses)
        }
    }

    fun onNonRecipientFieldFocused() {
        if (view.isVisible && view.getAddresses().isEmpty()) {
            view.isVisible = false
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_REPLY_TO_SHOWN, view.isVisible)
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        view.isVisible = savedInstanceState.getBoolean(STATE_KEY_REPLY_TO_SHOWN)
    }
}

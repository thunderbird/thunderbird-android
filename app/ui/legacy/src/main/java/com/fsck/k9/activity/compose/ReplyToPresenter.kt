package com.fsck.k9.activity.compose

import android.os.Bundle
import android.text.TextWatcher
import com.fsck.k9.FontSizes
import com.fsck.k9.Identity
import com.fsck.k9.mail.Address
import com.fsck.k9.view.RecipientSelectView.Recipient

private const val STATE_KEY_REPLY_TO_SHOWN = "com.fsck.k9.activity.compose.ReplyToPresenter.replyToShown"

class ReplyToPresenter(private val view: ReplyToView) {

    fun onSaveInstaceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_REPLY_TO_SHOWN, view.isVisible())
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        view.setVisible(savedInstanceState.getBoolean(STATE_KEY_REPLY_TO_SHOWN))
    }

    fun getAddresses(): Array<Address> {
        return view.getAddresses()
    }

    fun hasUncompletedRecipients(): Boolean {
        var result = false
        if (view.hasUncompletedText()) {
            view.showError()
            view.setVisible(true)
            result = true
        }
        return result
    }

    fun setIdentity(identity: Identity) {
        if (identity.replyTo != null && identity.replyTo?.length != 0) {
            val parsedAddresses = Address.parse(identity.replyTo)
            parsedAddresses.forEach { view.addRecipients(Recipient(it)) }
        }
    }

    fun onSwitchIdentity(oldIdentity: Identity, newIdentity: Identity) {
        if (oldIdentity.replyTo != null && oldIdentity.replyTo?.length != 0) {
            val recipients = view.getRecipients()
            val toRemove = recipients.filter { oldIdentity.replyTo!!.contains(it.address.address) }
            view.removeRecipients(toRemove)
        }
        setIdentity(newIdentity)
    }

    fun onNonRecipientFieldFocused() {
        if (view.isVisible() && view.getAddresses().size == 0) {
            view.setVisible(false)
        }
    }

    fun setFontSizes(fontSizes: FontSizes, fontSize: Int) {
        view.setFontSizes(fontSizes, fontSize)
    }

    fun addTextChangedListener(draftNeedsChangingTextWatcher: TextWatcher) {
        view.addTextChangedListener(draftNeedsChangingTextWatcher)
    }
}

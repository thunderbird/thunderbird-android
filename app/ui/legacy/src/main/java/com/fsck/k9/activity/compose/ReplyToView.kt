package com.fsck.k9.activity.compose

import android.text.TextWatcher
import android.view.View
import android.widget.ViewAnimator
import com.fsck.k9.FontSizes
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.R
import com.fsck.k9.view.RecipientSelectView
import com.fsck.k9.view.RecipientSelectView.Recipient

private const val VIEW_INDEX_REPLY_TO_EXPANDER_VISIBLE = 0
private const val VIEW_INDEX_REPLY_TO_EXPANDER_HIDDEN = 1

class ReplyToView(activity: MessageCompose) : View.OnClickListener {
    private val replyToView: RecipientSelectView
    private val replyToWrapper: View
    private val replyToDivider: View
    private val replyToExpanderContainer: ViewAnimator
    private val replyToExpander: View

    init {
        replyToView = activity.findViewById(R.id.reply_to)
        replyToDivider = activity.findViewById(R.id.reply_to_divider)
        replyToWrapper = activity.findViewById(R.id.reply_to_wrapper)
        replyToExpanderContainer = activity.findViewById(R.id.reply_to_expander_container)
        replyToExpander = activity.findViewById(R.id.reply_to_expander)
        replyToExpander.setOnClickListener(this)
        val replyToLabel: View = activity.findViewById(R.id.reply_to_label)
        replyToLabel.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        if (view?.getId() == R.id.reply_to_expander) {
            setVisible(true)
        } else if (view?.getId() == R.id.reply_to_label) {
            replyToView.requestFocus()
        }
    }

    fun isVisible(): Boolean {
        return replyToView.getVisibility() == View.VISIBLE
    }

    fun setVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        replyToDivider.visibility = visibility
        replyToView.visibility = visibility
        replyToWrapper.visibility = visibility
        if (visible && replyToExpanderContainer.displayedChild == VIEW_INDEX_REPLY_TO_EXPANDER_VISIBLE) {
            replyToView.requestFocus()
            replyToExpanderContainer.displayedChild = VIEW_INDEX_REPLY_TO_EXPANDER_HIDDEN
        } else if (replyToExpanderContainer.displayedChild == VIEW_INDEX_REPLY_TO_EXPANDER_HIDDEN) {
            replyToExpanderContainer.displayedChild = VIEW_INDEX_REPLY_TO_EXPANDER_VISIBLE
        }
    }

    fun hasUncompletedText(): Boolean {
        replyToView.tryPerformCompletion()
        return replyToView.hasUncompletedText()
    }

    fun showError() {
        replyToView.setError(replyToView.getContext().getString(R.string.compose_error_incomplete_recipient))
    }

    fun addRecipients(vararg recipient: Recipient) {
        replyToView.addRecipients(*recipient)
    }

    fun getAddresses(): Array<Address> {
        return replyToView.addresses
    }

    fun removeRecipients(recipients: List<Recipient>) {
        recipients.forEach { replyToView.removeObjectSync(it) }
    }

    fun setFontSizes(fontSizes: FontSizes, fontSize: Int) {
        val tokenTextSize: Int = getTokenTextSize(fontSize)
        replyToView.setTokenTextSize(tokenTextSize)
        fontSizes.setViewTextSize(replyToView, fontSize)
    }

    private fun getTokenTextSize(fontSize: Int): Int {
        return when (fontSize) {
            FontSizes.FONT_10SP -> FontSizes.FONT_10SP
            FontSizes.FONT_12SP -> FontSizes.FONT_12SP
            FontSizes.SMALL -> FontSizes.SMALL
            FontSizes.FONT_16SP -> 15
            FontSizes.MEDIUM -> FontSizes.FONT_16SP
            FontSizes.FONT_20SP -> FontSizes.MEDIUM
            FontSizes.LARGE -> FontSizes.FONT_20SP
            else -> FontSizes.FONT_DEFAULT
        }
    }

    fun addTextChangedListener(draftNeedsChangingTextWatcher: TextWatcher) {
        replyToView.addTextChangedListener(draftNeedsChangingTextWatcher)
    }

    fun getRecipients(): List<Recipient> {
        return replyToView.objects
    }
}

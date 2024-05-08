package com.fsck.k9.activity.compose

import android.text.TextWatcher
import android.view.View
import android.widget.ViewAnimator
import androidx.core.view.isVisible
import com.fsck.k9.FontSizes
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.R
import com.fsck.k9.view.RecipientSelectView
import com.fsck.k9.view.RecipientSelectView.Recipient

private const val VIEW_INDEX_REPLY_TO_EXPANDER_VISIBLE = 0
private const val VIEW_INDEX_REPLY_TO_EXPANDER_HIDDEN = 1

class ReplyToView(activity: MessageCompose) {
    private val replyToView: RecipientSelectView = activity.findViewById(R.id.reply_to)
    private val replyToWrapper: View = activity.findViewById(R.id.reply_to_wrapper)
    private val replyToExpanderContainer: ViewAnimator = activity.findViewById(R.id.reply_to_expander_container)
    private val replyToExpander: View = activity.findViewById(R.id.reply_to_expander)

    private val textWatchers = mutableSetOf<TextWatcher>()

    init {
        replyToExpander.setOnClickListener {
            isVisible = true
            replyToView.requestFocus()
        }

        activity.findViewById<View>(R.id.reply_to_label).setOnClickListener {
            replyToView.requestFocus()
        }
    }

    var isVisible: Boolean
        get() = replyToWrapper.isVisible
        set(visible) {
            replyToView.isVisible = visible
            replyToWrapper.isVisible = visible

            if (visible && replyToExpanderContainer.displayedChild == VIEW_INDEX_REPLY_TO_EXPANDER_VISIBLE) {
                replyToExpanderContainer.displayedChild = VIEW_INDEX_REPLY_TO_EXPANDER_HIDDEN
            } else if (replyToExpanderContainer.displayedChild == VIEW_INDEX_REPLY_TO_EXPANDER_HIDDEN) {
                replyToExpanderContainer.displayedChild = VIEW_INDEX_REPLY_TO_EXPANDER_VISIBLE
            }
        }

    fun hideIfBlank() {
        if (isVisible && replyToView.text.isBlank()) {
            isVisible = false
        }
    }

    fun hasUncompletedText(): Boolean {
        replyToView.tryPerformCompletion()
        return replyToView.hasUncompletedText()
    }

    fun showError() {
        replyToView.error = replyToView.context.getString(R.string.compose_error_incomplete_recipient)
    }

    fun getAddresses(): Array<Address> {
        return replyToView.addresses
    }

    fun silentlyAddAddresses(addresses: Array<Address>) {
        removeAllTextChangedListeners()

        val recipients = addresses.map { Recipient(it) }.toTypedArray()
        replyToView.addRecipients(*recipients)

        addAllTextChangedListeners()
    }

    fun silentlyRemoveAddresses(addresses: Array<Address>) {
        val addressSet = addresses.toSet()
        val recipientsToRemove = replyToView.objects.filter { it.address in addressSet }

        if (recipientsToRemove.isNotEmpty()) {
            removeAllTextChangedListeners()

            for (recipient in recipientsToRemove) {
                replyToView.removeObjectSync(recipient)
            }

            addAllTextChangedListeners()
        }
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

    fun addTextChangedListener(textWatcher: TextWatcher) {
        textWatchers.add(textWatcher)
        replyToView.addTextChangedListener(textWatcher)
    }

    private fun removeAllTextChangedListeners() {
        for (textWatcher in textWatchers) {
            replyToView.removeTextChangedListener(textWatcher)
        }
    }

    private fun addAllTextChangedListeners() {
        for (textWatcher in textWatchers) {
            replyToView.addTextChangedListener(textWatcher)
        }
    }
}

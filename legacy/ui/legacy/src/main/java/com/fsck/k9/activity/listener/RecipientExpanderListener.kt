package com.fsck.k9.activity.listener

import com.fsck.k9.helper.SimpleTextWatcher

class RecipientExpanderListener(
    private val isRecipientExpanderExpanded: () -> Boolean,
    private val onBeforeTextChanged: () -> Unit,
) : SimpleTextWatcher() {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        if (isRecipientExpanderExpanded()) {
            onBeforeTextChanged()
        }
    }
}

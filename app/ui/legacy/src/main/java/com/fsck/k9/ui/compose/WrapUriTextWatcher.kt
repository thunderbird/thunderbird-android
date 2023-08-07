package com.fsck.k9.ui.compose

import android.text.Editable
import android.text.TextWatcher
import com.fsck.k9.message.html.UriMatcher

private const val NO_INDEX = -1
private const val MINIMUM_URI_LENGTH = 2 // scheme name + colon

/**
 * Wraps inserted URIs in angle brackets.
 */
class WrapUriTextWatcher : TextWatcher {
    private var insertedStartIndex = NO_INDEX
    private var insertedEndIndex = NO_INDEX

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s == null || count < MINIMUM_URI_LENGTH) {
            insertedStartIndex = NO_INDEX
        } else {
            insertedStartIndex = start
            insertedEndIndex = start + count
        }
    }

    override fun afterTextChanged(s: Editable?) {
        // Changing s below will lead to this TextWatcher being invoked again. Keep necessary state local.
        val insertedStartIndex = insertedStartIndex
        val insertedEndIndex = insertedEndIndex

        if (s != null && insertedStartIndex != NO_INDEX) {
            val insertedText = s.subSequence(insertedStartIndex, insertedEndIndex)
            if (UriMatcher.isValidUri(insertedText)) {
                s.insert(insertedEndIndex, ">")
                s.insert(insertedStartIndex, "<")
            }
        }
    }
}

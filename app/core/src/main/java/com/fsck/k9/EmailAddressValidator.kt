package com.fsck.k9

import android.text.util.Rfc822Tokenizer
import android.widget.AutoCompleteTextView.Validator

import java.util.regex.Pattern

class EmailAddressValidator : Validator {

    override fun fixText(invalidText: CharSequence): CharSequence = ""

    override fun isValid(text: CharSequence): Boolean = Rfc822Tokenizer.tokenize(text).isNotEmpty()

    fun isValidAddressOnly(text: CharSequence): Boolean = EMAIL_ADDRESS_PATTERN.matcher(text).matches()

    companion object {
        private val EMAIL_ADDRESS_PATTERN = Pattern.compile(
                "[a-zA-Z0-9\\+\\.\\_\\%\\-]{1,256}" +
                        "\\@" +
                        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                        "(" +
                        "\\." +
                        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                        ")+"
        )
    }
}

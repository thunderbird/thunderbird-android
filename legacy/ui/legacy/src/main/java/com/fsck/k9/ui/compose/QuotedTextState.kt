package com.fsck.k9.ui.compose

interface QuotedTextState {
    fun includeQuotedText(): Boolean
    fun isForcePlainText(): Boolean
    fun isQuotedTextText(): Boolean
}

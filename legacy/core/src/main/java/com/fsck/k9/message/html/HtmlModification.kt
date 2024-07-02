package com.fsck.k9.message.html

internal abstract class HtmlModification private constructor(val startIndex: Int, val endIndex: Int) {
    abstract class Wrap(startIndex: Int, endIndex: Int) : HtmlModification(startIndex, endIndex) {
        abstract fun appendPrefix(textToHtml: TextToHtml)
        abstract fun appendSuffix(textToHtml: TextToHtml)
    }

    abstract class Replace(startIndex: Int, endIndex: Int) : HtmlModification(startIndex, endIndex) {
        abstract fun replace(textToHtml: TextToHtml)
    }
}

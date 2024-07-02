package com.fsck.k9.message.html

import org.jsoup.Jsoup

/**
 * Contains common routines to convert html to text and vice versa.
 */
object HtmlConverter {
    /**
     * When generating previews, Spannable objects that can't be converted into a String are
     * represented as 0xfffc. When displayed, these show up as undisplayed squares. These constants
     * define the object character and the replacement character.
     */
    private const val PREVIEW_OBJECT_CHARACTER = 0xfffc.toChar()
    private const val PREVIEW_OBJECT_REPLACEMENT = 0x20.toChar() // space

    /**
     * toHtml() converts non-breaking spaces into the UTF-8 non-breaking space, which doesn't get
     * rendered properly in some clients. Replace it with a simple space.
     */
    private const val NBSP_CHARACTER = 0x00a0.toChar() // utf-8 non-breaking space
    private const val NBSP_REPLACEMENT = 0x20.toChar() // space

    /**
     * Convert an HTML string to a plain text string.
     */
    @JvmStatic
    fun htmlToText(html: String): String {
        val document = Jsoup.parse(html)
        return HtmlToPlainText.toPlainText(document.body())
            .replace(PREVIEW_OBJECT_CHARACTER, PREVIEW_OBJECT_REPLACEMENT)
            .replace(NBSP_CHARACTER, NBSP_REPLACEMENT)
    }

    /**
     * Convert a text string into an HTML document.
     *
     * No HTML headers or footers are added to the result.  Headers and footers are added at display time.
     */
    @JvmStatic
    fun textToHtml(text: String): String {
        return EmailTextToHtml.convert(text)
    }

    /**
     * Convert a plain text string into an HTML fragment.
     */
    @JvmStatic
    fun textToHtmlFragment(text: String): String {
        return TextToHtml.toHtmlFragment(text, retainOriginalWhitespace = false)
    }
}

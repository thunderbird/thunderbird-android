package com.fsck.k9.message.html


import com.fsck.k9.K9
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
    private const val PREVIEW_OBJECT_REPLACEMENT = 0x20.toChar()  // space

    /**
     * toHtml() converts non-breaking spaces into the UTF-8 non-breaking space, which doesn't get
     * rendered properly in some clients. Replace it with a simple space.
     */
    private const val NBSP_CHARACTER = 0x00a0.toChar()    // utf-8 non-breaking space
    private const val NBSP_REPLACEMENT = 0x20.toChar()    // space


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

    @JvmStatic
    fun wrapStatusMessage(status: CharSequence): String {
        return wrapMessageContent("<div style=\"text-align:center; color: grey;\">$status</div>")
    }

    @JvmStatic
    fun wrapMessageContent(messageContent: CharSequence): String {
        // Include a meta tag so the WebView will not use a fixed viewport width of 980 px
        return "<html dir=\"auto\"><head><meta name=\"viewport\" content=\"width=device-width\"/>" +
                cssStyleTheme() +
                cssStylePre() +
                "</head><body>" +
                messageContent +
                "</body></html>"
    }

    @JvmStatic
    fun cssStyleTheme(): String {
        return if (K9.k9MessageViewTheme === K9.Theme.DARK) {
            "<style type=\"text/css\">" +
                    "* { background: black ! important; color: #F3F3F3 !important }" +
                    ":link, :link * { color: #CCFF33 !important }" +
                    ":visited, :visited * { color: #551A8B !important }</style> "
        } else {
            ""
        }
    }

    /**
     * Dynamically generate a CSS style for `<pre>` elements.
     *
     * The style incorporates the user's current preference setting for the font family used for plain text messages.
     *
     * @return A `<style>` element that can be dynamically included in the HTML `<head>` element when messages are
     * displayed.
     */
    @JvmStatic
    fun cssStylePre(): String {
        val font = if (K9.isUseMessageViewFixedWidthFont) "monospace" else "sans-serif"

        return "<style type=\"text/css\"> pre." + EmailTextToHtml.K9MAIL_CSS_CLASS +
                " {white-space: pre-wrap; word-wrap:break-word; " +
                "font-family: " + font + "; margin-top: 0px}</style>"
    }

    /**
     * Convert a plain text string into an HTML fragment.
     */
    @JvmStatic
    fun textToHtmlFragment(text: String): String {
        return TextToHtml.toHtmlFragment(text)
    }
}

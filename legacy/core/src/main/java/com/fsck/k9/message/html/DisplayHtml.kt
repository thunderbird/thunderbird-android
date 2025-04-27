package com.fsck.k9.message.html

import app.k9mail.html.cleaner.HtmlHeadProvider

class DisplayHtml(private val settings: HtmlSettings) : HtmlHeadProvider {
    override val headHtml: String
        get() {
            return """<meta name="viewport" content="width=device-width"/>""" +
                cssStylePre() +
                cssStyleSignature()
        }

    fun wrapStatusMessage(status: CharSequence): String {
        return wrapMessageContent("<div style=\"text-align:center; color: grey;\">$status</div>")
    }

    fun wrapMessageContent(messageContent: CharSequence): String {
        // Include a meta tag so the WebView will not use a fixed viewport width of 980 px
        return "<html dir=\"auto\"><head><meta name=\"viewport\" content=\"width=device-width\"/>" +
            cssStylePre() +
            "</head><body>" +
            messageContent +
            "</body></html>"
    }

    /**
     * Dynamically generate a CSS style for `<pre>` elements.
     *
     * The style incorporates the user's current preference setting for the font family used for plain text messages.
     *
     * @return A `<style>` element that can be dynamically included in the HTML `<head>` element when messages are
     * displayed.
     */
    private fun cssStylePre(): String {
        val font = if (settings.useFixedWidthFont) "monospace" else "sans-serif"

        return "<style type=\"text/css\"> pre." + EmailTextToHtml.K9MAIL_CSS_CLASS +
            " {white-space: pre-wrap; word-wrap:break-word; " +
            "font-family: " + font + "; margin-top: 0px}</style>"
    }

    private fun cssStyleSignature(): String {
        return """<style type="text/css">.k9mail-signature { opacity: 0.5 }</style>"""
    }
}

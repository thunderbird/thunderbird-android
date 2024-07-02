package com.fsck.k9.message.html

class DisplayHtmlFactory {
    fun create(settings: HtmlSettings): DisplayHtml {
        return DisplayHtml(settings)
    }
}

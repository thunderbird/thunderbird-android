package com.fsck.k9.message.html

import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist as AllowList

object HtmlHelper {
    @JvmStatic
    fun extractText(html: String): String {
        return Jsoup.clean(html, AllowList.none())
    }
}

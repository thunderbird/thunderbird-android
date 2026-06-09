package com.fsck.k9.message.html

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

/**
 * Sanitizes user-supplied HTML signatures before they are inserted into outgoing mail.
 *
 * Uses a Jsoup [Safelist.relaxed] baseline (common formatting tags, images, links, tables)
 * and tightens it so scripting constructs cannot survive a round-trip through the signature
 * field. Specifically, all `on*` event-handler attributes and `javascript:` URLs are removed,
 * and `<script>`, `<style>`, `<iframe>`, `<object>`, and `<embed>` elements are not in the
 * allowlist to begin with.
 */
object HtmlSignatureSanitizer {
    private val safelist: Safelist = Safelist.relaxed()
        .addAttributes(":all", "style", "class", "dir")
        .addProtocols("a", "href", "http", "https", "mailto", "tel")
        .addProtocols("img", "src", "http", "https", "data", "cid")

    @JvmStatic
    fun sanitize(html: String): String {
        if (html.isEmpty()) return html
        return Jsoup.clean(html, "", safelist)
    }
}

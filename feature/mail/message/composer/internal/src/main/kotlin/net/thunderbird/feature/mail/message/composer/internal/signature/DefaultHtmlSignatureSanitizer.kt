package net.thunderbird.feature.mail.message.composer.internal.signature

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.safety.Safelist
import net.thunderbird.feature.mail.message.composer.signature.HtmlSignatureSanitizer

/**
 * Sanitizes user-supplied HTML signatures before they are inserted into outgoing mail.
 *
 * Uses a Ksoup [Safelist.relaxed] baseline (common formatting tags, images, links, tables)
 * and tightens it so scripting constructs cannot survive a round-trip through the signature
 * field. Specifically, all `on*` event-handler attributes and `javascript:` URLs are removed,
 * and `<script>`, `<style>`, `<iframe>`, `<object>`, and `<embed>` elements are not in the
 * allowlist to begin with.
 */
internal class DefaultHtmlSignatureSanitizer : HtmlSignatureSanitizer {
    private val safelist: Safelist = Safelist.relaxed()
        .addAttributes(":all", "style", "class", "dir")
        .addProtocols("a", "href", "http", "https", "mailto", "tel")
        .addProtocols("img", "src", "http", "https", "data", "cid")

    override fun sanitize(html: String): String =
        if (html.isEmpty()) html else Ksoup.clean(bodyHtml = html, safelist = safelist)
}

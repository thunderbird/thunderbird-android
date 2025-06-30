package com.fsck.k9.message.html

import assertk.Assert
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.jsoup.Jsoup
import org.junit.Test

class DisplayHtmlTest {
    val displayHtml = DisplayHtml(HtmlSettings(useDarkMode = false, useFixedWidthFont = false))

    @Test
    fun wrapMessageContent_addsViewportMetaElement() {
        val html = displayHtml.wrapMessageContent("Some text")

        assertThat(html).containsHtmlElement("head > meta[name=viewport]")
    }

    @Test
    fun wrapMessageContent_setsDirToAuto() {
        val html = displayHtml.wrapMessageContent("Some text")

        assertThat(html).containsHtmlElement("html[dir=auto]")
    }

    @Test
    fun wrapMessageContent_addsPreCSS() {
        val html = displayHtml.wrapMessageContent("Some text")

        assertThat(html).containsHtmlElement("head > style")
    }

    @Test
    fun wrapMessageContent_putsMessageContentInBody() {
        val content = "Some text"

        val html = displayHtml.wrapMessageContent(content)

        assertThat(html).bodyText().isEqualTo(content)
    }

    @Test
    fun wrapMessageContent_addsGlobalStyleRules() {
        val html = displayHtml.wrapMessageContent("test")

        assertThat(html).containsStyleRulesFor(
            selector = "*",
            "word-break: break-word;",
            "overflow-wrap: break-word;",
        )
    }

    private fun Assert<String>.containsStyleRulesFor(selector: String, vararg expectedRules: String) = given { html ->
        val styleContent = Jsoup.parse(html)
            .select("style")
            .joinToString("\n") { it.data() }

        val selectorPattern = Regex.escape(selector).replace("\\*", "\\\\*")
        val selectorBlock = Regex("$selectorPattern\\s*\\{([^}]*)\\}", RegexOption.MULTILINE)
            .find(styleContent)
            ?.groupValues?.get(1)
            ?.trim()

        requireNotNull(selectorBlock) { "No style block found for selector: $selector" }

        expectedRules.forEach { rule ->
            assertThat(selectorBlock).contains(rule)
        }
    }

    private fun Assert<String>.containsHtmlElement(cssQuery: String) = given { actual ->
        assertThat(actual).htmlElements(cssQuery).hasSize(1)
    }

    private fun Assert<String>.htmlElements(cssQuery: String) = transform { html ->
        Jsoup.parse(html).select(cssQuery)
    }

    private fun Assert<String>.bodyText() = transform { html ->
        Jsoup.parse(html).body().text()
    }
}

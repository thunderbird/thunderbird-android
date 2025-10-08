package com.fsck.k9.message.html

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.jsoup.Jsoup
import org.junit.Test

@Suppress("LongMethod")
class DisplayHtmlTest {
    val htmlSettings = HtmlSettings(useDarkMode = false, useFixedWidthFont = false)
    val displayHtml = DisplayHtml(htmlSettings)

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
    fun wrapMessageContent_addsPreCSSStyles() {
        val html = displayHtml.wrapMessageContent("Some text")

        assertThat(html).containsHtmlElement("head > style", 3)
    }

    @Test
    fun wrapMessageContent_addsGlobalStyleRules() {
        val html = displayHtml.wrapMessageContent("test")

        val styleContent = Jsoup.parse(html)
            .select("style")
            .joinToString("\n") { it.data().trimIndent() }

        assertThat(styleContent).all {
            contains(
                """
                    |body { font-size: 0.9rem; }
                """.trimMargin(),
            )
            contains(
                """
                    |.clear:after {
                    |  content: "";
                    |  clear: both;
                    |  display: block;
                    |}
                """.trimMargin(),
            )
            contains(
                """
                    |.message {
                    |  display: block;
                    |  user-select: auto;
                    |  -webkit-user-select: auto;
                    |}
                """.trimMargin(),
            )
            contains(
                """
                    |.message.message-content {
                    |  width: 100%;
                    |  overflow-wrap: break-word;
                    |}
                """.trimMargin(),
            )
            contains(
                """
                    |.message.message-content pre { white-space: pre-wrap; }
                """.trimMargin(),
            )
            contains(
                """
                    |.message.message-content blockquote {
                    |  margin-left: .8ex !important;
                    |  margin-right: 0 !important;
                    |  border-left: 1px #ccc solid !important;
                    |  padding-left: 1ex !important
                    |}
                """.trimMargin(),
            )
            contains(
                """
                    |.message.message-content pre,
                    |.message.message-content code,
                    |.message.message-content table {
                    |  max-width: 100%;
                    |  overflow-x: auto;
                    |}
                """.trimMargin(),
            )
            contains(
                """
                    |body, div, section, article, main, header, footer {
                    |  overflow-x: hidden;
                    |}
                """.trimMargin(),
            )
            contains(
                """
                    |div.table-wrapper:has(table) {
                    |  overflow-x: auto;
                    |  display: block;
                    |  width: 100% !important;
                    |}
                """.trimMargin(),
            )
        }
    }

    @Test
    fun wrapMessageContent_addsPreCSS() {
        val html = displayHtml.wrapMessageContent("test")
        val expectedFont = if (htmlSettings.useFixedWidthFont) "monospace" else "sans-serif"

        assertThat(html).containsStyleRulesFor(
            selector = "pre.${EmailTextToHtml.K9MAIL_CSS_CLASS}",
            "white-space: pre-wrap;",
            "word-wrap: break-word;",
            "font-family: $expectedFont;",
            "margin-top: 0px;",
        )
    }

    @Test
    fun wrapMessageContent_addsSignatureStyleRules() {
        val html = displayHtml.wrapMessageContent("test")

        assertThat(html).containsStyleRulesFor(
            selector = ".k9mail-signature",
            "opacity: 0.5;",
        )
    }

    @Test
    fun wrapMessageContent_putsMessageContentInBody() {
        val content = "Some text"

        val html = displayHtml.wrapMessageContent(content)

        assertThat(html).bodyText().isEqualTo(content)
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

    private fun Assert<String>.containsHtmlElement(cssQuery: String, expectedCount: Int = 1) = given { actual ->
        assertThat(actual).htmlElements(cssQuery).hasSize(expectedCount)
    }

    private fun Assert<String>.htmlElements(cssQuery: String) = transform { html ->
        Jsoup.parse(html).select(cssQuery)
    }

    private fun Assert<String>.bodyText() = transform { html ->
        Jsoup.parse(html).body().text()
    }
}

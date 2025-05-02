package com.fsck.k9.message.html

import assertk.Assert
import assertk.assertThat
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

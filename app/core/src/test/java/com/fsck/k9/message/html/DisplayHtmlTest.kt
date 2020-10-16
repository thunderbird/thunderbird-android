package com.fsck.k9.message.html

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayHtmlTest {
    val displayHtml = DisplayHtml(HtmlSettings(useDarkMode = false, useFixedWidthFont = false))

    @Test
    fun wrapMessageContent_addsViewportMetaElement() {
        val html = displayHtml.wrapMessageContent("Some text")

        assertHtmlContainsElement(html, "head > meta[name=viewport]")
    }

    @Test
    fun wrapMessageContent_setsDirToAuto() {
        val html = displayHtml.wrapMessageContent("Some text")

        assertHtmlContainsElement(html, "html[dir=auto]")
    }

    @Test
    fun wrapMessageContent_addsPreCSS() {
        val html = displayHtml.wrapMessageContent("Some text")

        assertHtmlContainsElement(html, "head > style")
    }

    @Test
    fun wrapMessageContent_whenDarkMessageViewTheme_addsDarkThemeCSS() {
        val darkModeDisplayHtml = DisplayHtml(HtmlSettings(useDarkMode = true, useFixedWidthFont = false))

        val html = darkModeDisplayHtml.wrapMessageContent("Some text")

        assertHtmlContainsElement(html, "head > style", 2)
    }

    @Test
    fun wrapMessageContent_putsMessageContentInBody() {
        val content = "Some text"

        val html = displayHtml.wrapMessageContent(content)

        assertEquals(content, Jsoup.parse(html).body().text())
    }

    private fun assertHtmlContainsElement(html: String, cssQuery: String, numberOfExpectedOccurrences: Int = 1) {
        val document = Jsoup.parse(html)
        val numberOfFoundElements = document.select(cssQuery).size
        assertEquals(
            "Expected to find '$cssQuery' $numberOfExpectedOccurrences time(s) in:\n$html",
            numberOfExpectedOccurrences, numberOfFoundElements
        )
    }
}

package com.fsck.k9.message.html

import assertk.Assert
import assertk.assertThat
import assertk.assertions.atLeast
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.GlobalCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.PlainTextMessagePreElementCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.SignatureCssStyleProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.Test

class DisplayHtmlTest {
    val displayHtml = DisplayHtml(
        htmlSettings = HtmlSettings(useDarkMode = false, useFixedWidthFont = false),
        cssClassNameProvider = FakeCssClassNameProvider(),
        cssStyleProviders = listOf(),
    )

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
        val expectedStyleCssRule = ".my-custom-pre-class { mock-property: mock-value }"
        val displayHtml = DisplayHtml(
            htmlSettings = HtmlSettings(useDarkMode = false, useFixedWidthFont = false),
            cssClassNameProvider = FakeCssClassNameProvider(),
            cssStyleProviders = listOf(
                GlobalCssStyleProvider.Factory { FakeGlobalCssStyleProvider() },
                PlainTextMessagePreElementCssStyleProvider.Factory {
                    FakePlainTextMessagePreElementCssStyleProvider(
                        style = "<style>$expectedStyleCssRule</style>",
                    )
                },
                SignatureCssStyleProvider.Factory { FakeSignatureCssStyleProvider() },
            ),
        )
        val html = displayHtml.wrapMessageContent("Some text")

        assertThat(html).given { raw ->
            val html = Jsoup.parse(raw)
            assertThat(html.select("head > style"))
                .atLeast(1) { element ->
                    element
                        .prop(Element::data)
                        .isEqualTo(expectedStyleCssRule)
                }
        }
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

    private class FakeCssClassNameProvider(
        override val defaultNamespaceClassName: String = "mock defaultNamespaceClassName",
        override val rootClassName: String = "mock rootClassName",
        override val mainContentClassName: String = "mock mainContentClassName",
        override val plainTextMessagePreClassName: String = "mock plainTextMessagePreClassName",
        override val signatureClassName: String = "mock signatureClassName",
    ) : CssClassNameProvider

    private class FakeGlobalCssStyleProvider(
        override val style: String = "<style>.mock-style { mock-property: mock-value }</style>",
    ) : GlobalCssStyleProvider

    private class FakePlainTextMessagePreElementCssStyleProvider(
        override val style: String = "<style>.mock-style { mock-property: mock-value }</style>",
    ) : PlainTextMessagePreElementCssStyleProvider

    private class FakeSignatureCssStyleProvider(
        override val style: String = "<style>.mock-style { mock-property: mock-value }</style>",
    ) : SignatureCssStyleProvider
}

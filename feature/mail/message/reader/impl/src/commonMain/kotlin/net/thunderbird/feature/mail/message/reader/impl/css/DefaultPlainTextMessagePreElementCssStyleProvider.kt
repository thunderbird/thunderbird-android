package net.thunderbird.feature.mail.message.reader.impl.css

import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.PlainTextMessagePreElementCssStyleProvider
import org.intellij.lang.annotations.Language

class DefaultPlainTextMessagePreElementCssStyleProvider(
    cssClassNameProvider: CssClassNameProvider,
    useFixedWidthFont: Boolean,
) : PlainTextMessagePreElementCssStyleProvider {

    @Language("HTML")
    override val style: String = """
        |<style>
        |  pre.${cssClassNameProvider.plainTextMessagePreClassName} {
        |    white-space: pre-wrap;
        |    word-wrap: break-word;
        |    font-family: ${if (useFixedWidthFont) "monospace" else "sans-serif"};
        |    margin-top: 0px;
        |  }
        |</style>
    """.trimMargin()

    class Factory(
        private val cssClassNameProvider: CssClassNameProvider,
    ) : PlainTextMessagePreElementCssStyleProvider.Factory {
        override fun create(htmlSettings: HtmlSettings): PlainTextMessagePreElementCssStyleProvider {
            return DefaultPlainTextMessagePreElementCssStyleProvider(
                cssClassNameProvider = cssClassNameProvider,
                useFixedWidthFont = htmlSettings.useFixedWidthFont,
            )
        }
    }
}

package net.thunderbird.feature.mail.message.reader.impl.css

import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssVariableNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.GlobalCssStyleProvider
import org.intellij.lang.annotations.Language

internal class DefaultGlobalCssStyleProvider private constructor(
    cssClassNameProvider: CssClassNameProvider,
    cssVariableNameProvider: CssVariableNameProvider,
) : GlobalCssStyleProvider {
    @Language("HTML")
    override val style: String = """
        |<style>
        |  body { font-size: 0.9rem; }
        |  .clear:after {
        |    content: "";
        |    clear: both;
        |    display: block;
        |  }
        |  .${cssClassNameProvider.rootClassName} {
        |    display: block;
        |    user-select: auto;
        |    -webkit-user-select: auto;
        |  }
        |  .${cssClassNameProvider.rootClassName}.${cssClassNameProvider.mainContentClassName} {
        |    width: 100%;
        |    overflow-wrap: break-word;
        |    padding: 0 8px;
        |  }
        |  .${cssClassNameProvider.rootClassName}.${cssClassNameProvider.mainContentClassName} pre {
        |    white-space: pre-wrap;
        |  }
        |  .${cssClassNameProvider.rootClassName}.${cssClassNameProvider.mainContentClassName} blockquote {
        |    margin: auto 0 auto 0.8ex !important;
        |    padding-left: 1ex !important;
        |    border-left-width: 1px !important;
        |    border-left-style: solid !important;
        |    border-left-color: var(${cssVariableNameProvider.blockquoteDefaultBorderLeftColor}, #ccc);
        |  }
        |</style>
    """.trimMargin()

    internal class Factory(
        private val cssClassNameProvider: CssClassNameProvider,
        private val cssVariableNameProvider: CssVariableNameProvider,
    ) : GlobalCssStyleProvider.Factory {
        override fun create(htmlSettings: HtmlSettings): CssStyleProvider = DefaultGlobalCssStyleProvider(
            cssClassNameProvider = cssClassNameProvider,
            cssVariableNameProvider = cssVariableNameProvider,
        )
    }
}

internal class LegacyGlobalCssStyleProvider(useDarkMode: Boolean) : GlobalCssStyleProvider {
    @Language("HTML")
    override val style: String = if (useDarkMode) {
        """
        |<style type="text/css">
        |  * {
        |    background: #121212 !important;
        |    color: #F3F3F3 !important
        |  }
        |  :link, :link * { color: #CCFF33 !important }
        |  :visited, :visited * { color: #551A8B !important }
        |</style>
        """.trimMargin()
    } else {
        ""
    }

    internal class Factory : GlobalCssStyleProvider.Factory {
        override fun create(htmlSettings: HtmlSettings): CssStyleProvider = LegacyGlobalCssStyleProvider(
            useDarkMode = htmlSettings.useDarkMode,
        )
    }
}

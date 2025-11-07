package net.thunderbird.feature.mail.message.reader.impl.css

import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.SignatureCssStyleProvider
import org.intellij.lang.annotations.Language

class DefaultSignatureCssStyleProvider(
    cssClassNameProvider: CssClassNameProvider,
) : SignatureCssStyleProvider {
    @Language("HTML")
    override val style: String = """
        |<style>
        |  .${cssClassNameProvider.signatureClassName} {
        |    opacity: 0.5;
        |  }
        |</style>
    """.trimMargin()

    class Factory(private val cssClassNameProvider: CssClassNameProvider) : SignatureCssStyleProvider.Factory {
        override fun create(htmlSettings: HtmlSettings): CssStyleProvider =
            DefaultSignatureCssStyleProvider(cssClassNameProvider)
    }
}

package net.thunderbird.feature.mail.message.reader.impl.css

import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssVariableNameProvider

internal class DefaultCssVariableNameProvider(
    cssClassNameProvider: CssClassNameProvider,
) : CssVariableNameProvider {
    override val blockquoteDefaultBorderLeftColor: String =
        "--${cssClassNameProvider.defaultNamespaceClassName}__blockquote-default-border-color"
}

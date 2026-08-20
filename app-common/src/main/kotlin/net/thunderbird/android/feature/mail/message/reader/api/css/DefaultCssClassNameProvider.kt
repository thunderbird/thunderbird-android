package net.thunderbird.android.feature.mail.message.reader.api.css

import com.fsck.k9.message.html.EmailTextToHtml
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider

class DefaultCssClassNameProvider(
    featureFlagProvider: FeatureFlagProvider,
    override val defaultNamespaceClassName: String,
) : CssClassNameProvider {
    override val rootClassName: String = "${defaultNamespaceClassName}__message-viewer"
    override val mainContentClassName: String = "${defaultNamespaceClassName}__main-content"
    override val plainTextMessagePreClassName: String =
        if (featureFlagProvider.provide(GeneratedFeatureFlagKey.USE_NEW_MESSAGE_READER_CSS_STYLES).isEnabled()) {
            "${defaultNamespaceClassName}__plain-text-message-pre"
        } else {
            // TODO(#10498): Remove when UseNewMessageReaderCssStyles is no longer required
            EmailTextToHtml.K9MAIL_CSS_CLASS
        }
    override val signatureClassName: String =
        if (featureFlagProvider.provide(GeneratedFeatureFlagKey.USE_NEW_MESSAGE_READER_CSS_STYLES).isEnabled()) {
            "${defaultNamespaceClassName}__signature"
        } else {
            // TODO(#10498): Remove when UseNewMessageReaderCssStyles is no longer required
            "k9mail-signature"
        }
}

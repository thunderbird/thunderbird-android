package net.thunderbird.android.feature.mail.message.reader.api.css

import com.fsck.k9.message.html.EmailTextToHtml
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.feature.mail.message.reader.api.MessageReaderFeatureFlags
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider

class DefaultCssClassNameProvider(
    featureFlagProvider: FeatureFlagProvider,
    override val defaultNamespaceClassName: String,
) : CssClassNameProvider {
    override val rootClassName: String = "${defaultNamespaceClassName}__message-viewer"
    override val mainContentClassName: String = "${defaultNamespaceClassName}__main-content"
    override val plainTextMessagePreClassName: String =
        if (featureFlagProvider.provide(MessageReaderFeatureFlags.UseNewMessageReaderCssStyles).isEnabled()) {
            EmailTextToHtml.K9MAIL_CSS_CLASS
        } else {
            "${defaultNamespaceClassName}__plain-text-message-pre"
        }
    override val signatureClassName: String =
        if (featureFlagProvider.provide(MessageReaderFeatureFlags.UseNewMessageReaderCssStyles).isEnabled()) {
            "${defaultNamespaceClassName}__signature"
        } else {
            "k9mail-signature"
        }
}

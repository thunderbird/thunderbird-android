package net.thunderbird.feature.mail.message.reader.api

import net.thunderbird.core.featureflag.FeatureFlagKey

object MessageReaderFeatureFlags {
    // TODO(#10498): Remove when UseNewMessageReaderCssStyles is no longer required
    val UseNewMessageReaderCssStyles = FeatureFlagKey("use_new_message_reader_css_styles")
}

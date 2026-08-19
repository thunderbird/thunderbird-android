package net.thunderbird.feature.mail.message.reader.api

import net.thunderbird.core.featureflag.FeatureFlagKey

object MessageReaderFeatureFlags {
    // TODO(#10498): Remove when UseNewMessageReaderCssStyles is no longer required
    val UseComposeForMessageReader = FeatureFlagKey("use_compose_for_message_reader")
}

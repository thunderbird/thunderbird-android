package app.k9mail.featureflag

import net.thunderbird.core.featureflag.model.AppVariantOverrides
import net.thunderbird.core.featureflag.model.BaseAppVariantOverrides
import net.thunderbird.core.featureflag.model.FlagOverrides

/**
 * K-9 Mail-specific implementation of app variant feature flag overrides.
 *
 * This class provides K-9 Mail's configuration for feature flag overrides across
 * different build variants (debug and release). It wraps a map of variant-specific
 * flag overrides and exposes them through the standard AppVariantOverrides interface.
 *
 * @param wrapper Map containing variant names as keys and their corresponding FlagOverrides.
 */
class K9MailOverrides(wrapper: Map<String, FlagOverrides>) : BaseAppVariantOverrides(wrapper) {
    companion object {
        val Factory = AppVariantOverrides.Factory { wrapper -> K9MailOverrides(wrapper) }
    }
}

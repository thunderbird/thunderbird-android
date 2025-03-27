package net.thunderbird.app.common.account

import app.k9mail.core.featureflag.FeatureFlagProvider
import app.k9mail.core.featureflag.toFeatureFlagKey
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.AccountDefaultsProvider

class CommonAccountDefaultsProvider(
    private val featureFlagProvider: FeatureFlagProvider,
) : AccountDefaultsProvider {

    override fun applyDefaults(account: Account) {
        account.isNotifyNewMail = featureFlagProvider.provide(
            "email_notification_default".toFeatureFlagKey(),
        ).whenEnabledOrNot(
            onEnabled = { true },
            onDisabledOrUnavailable = { false },
        )

        account.isNotifySelfNewMail = featureFlagProvider.provide(
            "email_notification_default".toFeatureFlagKey(),
        ).whenEnabledOrNot(
            onEnabled = { true },
            onDisabledOrUnavailable = { false },
        )
    }
}

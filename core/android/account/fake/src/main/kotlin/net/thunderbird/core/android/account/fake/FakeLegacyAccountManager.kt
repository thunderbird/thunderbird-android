package net.thunderbird.core.android.account.fake

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.mail.account.fake.FakeAccountManager

/**
 * In-memory [LegacyAccountManager] for tests. See [FakeAccountManager].
 */
open class FakeLegacyAccountManager(
    initialAccounts: List<LegacyAccount> = emptyList(),
) : FakeAccountManager<LegacyAccount>(initialAccounts), LegacyAccountManager

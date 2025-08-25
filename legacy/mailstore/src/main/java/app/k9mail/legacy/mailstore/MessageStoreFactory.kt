package app.k9mail.legacy.mailstore

import net.thunderbird.core.android.account.LegacyAccount

interface MessageStoreFactory {
    fun create(account: LegacyAccount): ListenableMessageStore
}

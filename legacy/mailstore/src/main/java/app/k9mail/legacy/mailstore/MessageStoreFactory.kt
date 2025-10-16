package app.k9mail.legacy.mailstore

import net.thunderbird.core.android.account.LegacyAccountDto

interface MessageStoreFactory {
    fun create(account: LegacyAccountDto): ListenableMessageStore
}

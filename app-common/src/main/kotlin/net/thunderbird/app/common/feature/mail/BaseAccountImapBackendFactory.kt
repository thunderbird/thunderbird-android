package net.thunderbird.app.common.feature.mail

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backends.ImapBackendFactory
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.storage.legacy.mapper.LegacyAccountDataMapper
import net.thunderbird.feature.mail.account.api.BaseAccount

class BaseAccountImapBackendFactory(
    private val legacyFactory: ImapBackendFactory,
    private val legacyMapper: LegacyAccountDataMapper,
) : BackendFactory<BaseAccount> {
    override fun createBackend(account: BaseAccount): Backend {
        val dto = when (account) {
            is LegacyAccountDto -> account
            is LegacyAccount -> legacyMapper.toDto(account)
            else -> error("Unsupported account type ${account::class.qualifiedName}")
        }
        return legacyFactory.createBackend(dto)
    }
}

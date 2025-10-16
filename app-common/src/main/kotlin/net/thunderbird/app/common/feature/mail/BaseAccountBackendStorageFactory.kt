package net.thunderbird.app.common.feature.mail

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.mailstore.LegacyAccountDtoBackendStorageFactory
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.storage.legacy.mapper.LegacyAccountDataMapper
import net.thunderbird.feature.mail.account.api.BaseAccount

/**
 * A [BackendStorageFactory] that supports both [LegacyAccountDto] and [LegacyAccount].
 */
class BaseAccountBackendStorageFactory(
    private val legacyFactory: LegacyAccountDtoBackendStorageFactory,
    private val legacyMapper: LegacyAccountDataMapper,
) : BackendStorageFactory<BaseAccount> {
    override fun createBackendStorage(account: BaseAccount): BackendStorage {
        return when (account) {
            is LegacyAccountDto -> legacyFactory.createBackendStorage(account)
            is LegacyAccount -> {
                val legacyAccountDto = legacyMapper.toDto(account)
                legacyFactory.createBackendStorage(legacyAccountDto)
            }
            else -> throw IllegalArgumentException("Unsupported account type: ${account::class.java.name}")
        }
    }
}

package net.thunderbird.app.common.feature.mail

import com.fsck.k9.mailstore.LegacyAccountDtoSpecialFolderUpdaterFactory
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.storage.legacy.mapper.LegacyAccountDataMapper
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

/**
 * A [SpecialFolderUpdater.Factory] that supports both [LegacyAccountDto] and [LegacyAccount].
 */
class BaseAccountSpecialFolderUpdaterFactory(
    private val legacyFactory: LegacyAccountDtoSpecialFolderUpdaterFactory,
    private val legacyMapper: LegacyAccountDataMapper,
) : SpecialFolderUpdater.Factory<BaseAccount> {
    override fun create(account: BaseAccount): SpecialFolderUpdater {
        return when (account) {
            is LegacyAccountDto -> legacyFactory.create(account)
            is LegacyAccount -> {
                val legacyAccountDto = legacyMapper.toDto(account)
                legacyFactory.create(legacyAccountDto)
            }

            else -> throw IllegalArgumentException("Unsupported account type: ${account::class.java.name}")
        }
    }
}

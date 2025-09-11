package net.thunderbird.feature.account.storage.legacy.mapper

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.architecture.data.DataMapper

/**
 * Mapper for converting between [LegacyAccount] and [LegacyAccountDto].
 */
interface LegacyAccountDataMapper : DataMapper<LegacyAccount, LegacyAccountDto>

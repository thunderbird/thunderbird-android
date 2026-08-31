package net.thunderbird.feature.mail.folder.api.data.repository

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.data.FolderError

interface FolderPushTrackingRepository {
    fun observeEnabled(
        accountId: AccountId,
    ): Flow<Outcome<Boolean, FolderError>>

    suspend fun isEnabled(accountId: AccountId): Outcome<Boolean, FolderError>

    suspend fun disable(
        accountId: AccountId,
    ): Outcome<Unit, FolderError>
}

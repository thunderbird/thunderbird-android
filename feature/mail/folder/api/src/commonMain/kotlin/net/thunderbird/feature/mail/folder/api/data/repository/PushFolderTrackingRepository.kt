package net.thunderbird.feature.mail.folder.api.data.repository

import kotlinx.coroutines.flow.Flow
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.data.FolderError

public interface PushFolderTrackingRepository {
    public fun observeEnabled(
        accountId: AccountId,
    ): Flow<Outcome<Boolean, FolderError>>

    public suspend fun isEnabled(accountId: AccountId): Outcome<Boolean, FolderError>

    public suspend fun disable(
        accountId: AccountId,
    ): Outcome<Unit, FolderError>
}

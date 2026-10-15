package app.k9mail.legacy.mailstore

import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderDetailsRepository

internal class FakeRemoteFolderDetailsRepository(
    var outcome: Outcome<List<RemoteFolderDetails>, FolderError> = Outcome.Companion.success(emptyList()),
) : RemoteFolderDetailsRepository {
    override suspend fun getAllByAccountId(accountId: AccountId): Outcome<List<RemoteFolderDetails>, FolderError> =
        outcome
}

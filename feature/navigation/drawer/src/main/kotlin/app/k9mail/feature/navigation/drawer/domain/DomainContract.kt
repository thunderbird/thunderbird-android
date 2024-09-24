package app.k9mail.feature.navigation.drawer.domain

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.legacy.account.Account
import kotlinx.coroutines.flow.Flow

internal interface DomainContract {

    interface UseCase {
        fun interface GetDrawerConfig {
            operator fun invoke(): Flow<DrawerConfig>
        }

        fun interface GetDisplayAccounts {
            operator fun invoke(): Flow<List<DisplayAccount>>
        }

        fun interface GetDisplayFoldersForAccount {
            operator fun invoke(accountUuid: String, includeUnifiedFolders: Boolean): Flow<List<DisplayFolder>>
        }

        /**
         * Synchronize mail for the given account.
         *
         * Account can be null to synchronize unified inbox or account list.
         */
        fun interface SyncMail {
            operator fun invoke(account: Account?): Flow<Result<Unit>>
        }
    }
}

package net.thunderbird.feature.navigation.drawer.dropdown.domain

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayUnifiedFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayUnifiedFolderType

internal interface DomainContract {

    interface UseCase {
        fun interface GetDrawerConfig {
            operator fun invoke(): Flow<DrawerConfig>
        }

        fun interface SaveDrawerConfig {
            operator fun invoke(drawerConfig: DrawerConfig): Flow<Unit>
        }

        fun interface GetDisplayAccounts {
            operator fun invoke(): Flow<List<DisplayAccount>>
        }

        fun interface GetDisplayFoldersForAccount {
            operator fun invoke(accountId: String, includeUnifiedFolders: Boolean): Flow<List<DisplayFolder>>
        }

        /**
         * Synchronize the given account uuid.
         */
        fun interface SyncAccount {
            operator fun invoke(accountUuid: String): Flow<Result<Unit>>
        }

        /**
         * Synchronize all accounts.
         */
        fun interface SyncAllAccounts {
            operator fun invoke(): Flow<Result<Unit>>
        }
    }

    interface UnifiedFolderRepository {
        fun getDisplayUnifiedFolderFlow(unifiedFolderType: DisplayUnifiedFolderType): Flow<DisplayUnifiedFolder>
    }
}

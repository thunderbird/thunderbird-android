package app.k9mail.feature.navigation.drawer.domain

import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.domain.entity.DrawerConfig
import app.k9mail.legacy.ui.folder.DisplayFolder
import kotlinx.coroutines.flow.Flow

interface DomainContract {

    interface UseCase {
        fun interface GetDrawerConfig {
            operator fun invoke(): Flow<DrawerConfig>
        }

        fun interface GetDisplayAccounts {
            operator fun invoke(): Flow<List<DisplayAccount>>
        }

        fun interface GetDisplayFoldersForAccount {
            operator fun invoke(accountUuid: String): Flow<List<DisplayFolder>>
        }
    }
}

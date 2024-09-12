package app.k9mail.feature.navigation.drawer.domain

import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import kotlinx.coroutines.flow.Flow

interface DomainContract {

    interface UseCase {
        fun interface GetDisplayAccounts {
            operator fun invoke(): Flow<List<DisplayAccount>>
        }
    }
}

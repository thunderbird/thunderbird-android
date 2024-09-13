package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfigLoader
import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.entity.DrawerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetDrawerConfig(
    private val configProver: DrawerConfigLoader,
) : UseCase.GetDrawerConfig {
    override operator fun invoke(): Flow<DrawerConfig> {
        return flow {
            emit(configProver.loadDrawerConfig())
        }
    }
}

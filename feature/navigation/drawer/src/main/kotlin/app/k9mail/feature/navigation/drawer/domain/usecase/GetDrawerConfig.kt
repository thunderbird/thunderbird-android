package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfigLoader
import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class GetDrawerConfig(
    private val configProver: DrawerConfigLoader,
) : UseCase.GetDrawerConfig {
    override operator fun invoke(): Flow<DrawerConfig> {
        // TODO This needs to be updated when the config changes
        return flow {
            emit(configProver.loadDrawerConfig())
        }
    }
}

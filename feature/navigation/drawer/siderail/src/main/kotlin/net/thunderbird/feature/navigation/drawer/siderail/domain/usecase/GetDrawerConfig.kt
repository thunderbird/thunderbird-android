package net.thunderbird.feature.navigation.drawer.siderail.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfigLoader
import net.thunderbird.feature.navigation.drawer.siderail.domain.DomainContract

internal class GetDrawerConfig(
    private val configLoader: DrawerConfigLoader,
) : DomainContract.UseCase.GetDrawerConfig {
    override operator fun invoke(): Flow<DrawerConfig> {
        return configLoader.loadDrawerConfigFlow()
    }
}

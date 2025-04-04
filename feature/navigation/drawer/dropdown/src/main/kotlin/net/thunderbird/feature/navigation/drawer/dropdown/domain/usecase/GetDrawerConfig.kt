package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfigLoader
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase

internal class GetDrawerConfig(
    private val configLoader: DrawerConfigLoader,
) : UseCase.GetDrawerConfig {
    override operator fun invoke(): Flow<DrawerConfig> {
        return configLoader.loadDrawerConfigFlow()
    }
}

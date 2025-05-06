package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfigWriter
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase

internal class SaveDrawerConfig(
    private val drawerConfigWriter: DrawerConfigWriter,
) : UseCase.SaveDrawerConfig {
    override fun invoke(drawerConfig: NavigationDrawerExternalContract.DrawerConfig): Flow<Unit> {
        return flow {
            emit(drawerConfigWriter.writeDrawerConfig(drawerConfig))
        }
    }
}

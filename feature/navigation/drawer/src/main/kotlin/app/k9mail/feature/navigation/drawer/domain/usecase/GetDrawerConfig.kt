package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfigLoader
import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.legacy.preferences.GeneralSettingsChangeListener
import app.k9mail.legacy.preferences.GeneralSettingsManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class GetDrawerConfig(
    private val configProver: DrawerConfigLoader,
    private val generalSettingsManager: GeneralSettingsManager,
) : UseCase.GetDrawerConfig {
    override operator fun invoke(): Flow<DrawerConfig> {
        return callbackFlow {
            send(configProver.loadDrawerConfig())

            val listener = GeneralSettingsChangeListener {
                trySendBlocking(configProver.loadDrawerConfig())
            }

            generalSettingsManager.addListener(listener)

            awaitClose {
                generalSettingsManager.removeListener(listener)
            }
        }.distinctUntilChanged()
    }
}

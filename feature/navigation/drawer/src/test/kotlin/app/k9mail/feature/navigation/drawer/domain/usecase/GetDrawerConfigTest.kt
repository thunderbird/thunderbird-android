package app.k9mail.feature.navigation.drawer.domain.usecase

import app.cash.turbine.turbineScope
import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

internal class GetDrawerConfigTest {
    private val drawerConfigLoader = FakeDrawerConfigLoader()
    private val generalSettingsManager = FakeGeneralSettingsManager()
    private val getDrawerConfig = GetDrawerConfig(
        configProver = drawerConfigLoader,
        generalSettingsManager = generalSettingsManager,
    )

    @Test
    fun `should get drawer config`() = runTest {
        setDrawerConfig(
            showUnifiedFolders = false,
            showStarredCount = true,
        )

        val result = getDrawerConfig().first()

        assertThat(result).isEqualTo(
            DrawerConfig(
                showUnifiedFolders = false,
                showStarredCount = true,
            ),
        )
    }

    @Test
    fun `changing drawer config should emit`() = runTest {
        setDrawerConfig(
            showUnifiedFolders = true,
            showStarredCount = true,
        )

        turbineScope {
            val drawerConfigTurbine = getDrawerConfig().testIn(backgroundScope)

            assertThat(drawerConfigTurbine.awaitItem()).isEqualTo(
                DrawerConfig(
                    showUnifiedFolders = true,
                    showStarredCount = true,
                ),
            )

            setDrawerConfig(
                showUnifiedFolders = true,
                showStarredCount = false,
            )

            assertThat(drawerConfigTurbine.awaitItem()).isEqualTo(
                DrawerConfig(
                    showUnifiedFolders = true,
                    showStarredCount = false,
                ),
            )
        }
    }

    private fun setDrawerConfig(
        showUnifiedFolders: Boolean,
        showStarredCount: Boolean,
    ) {
        drawerConfigLoader.drawerConfig = DrawerConfig(showUnifiedFolders, showStarredCount)
        generalSettingsManager.notifyListeners()
    }
}

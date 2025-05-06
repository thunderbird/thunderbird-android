package net.thunderbird.feature.navigation.drawer.siderail.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfigLoader
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

internal class GetDrawerConfigTest {

    @Test
    fun `should get drawer config`() = runTest {
        val configLoader: DrawerConfigLoader = mock()
        val drawerConfig = DrawerConfig(
            showUnifiedFolders = true,
            showStarredCount = true,
            showAccountSelector = true,
        )

        val testSubject = GetDrawerConfig(configLoader = configLoader)
        whenever(configLoader.loadDrawerConfigFlow()).thenReturn(flowOf(drawerConfig))

        val result = testSubject().first()

        assertThat(result).isEqualTo(drawerConfig)
    }
}

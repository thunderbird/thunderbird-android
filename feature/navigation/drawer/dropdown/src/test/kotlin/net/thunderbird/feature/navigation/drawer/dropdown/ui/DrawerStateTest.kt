package net.thunderbird.feature.navigation.drawer.dropdown.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State
import org.junit.Test

internal class DrawerStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                config = DrawerConfig(
                    showUnifiedFolders = false,
                    showStarredCount = false,
                    showAccountSelector = true,
                ),
                accounts = persistentListOf(),
                selectedAccountId = null,
                folders = persistentListOf(),
                selectedFolderId = null,
                isLoading = false,
            ),
        )
    }
}

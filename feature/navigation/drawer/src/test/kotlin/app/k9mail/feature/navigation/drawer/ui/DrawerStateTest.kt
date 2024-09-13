package app.k9mail.feature.navigation.drawer.ui

import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class DrawerStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                accounts = persistentListOf(),
                currentAccount = null,
                folders = persistentListOf(),
                selectedFolder = null,
                showStarredCount = false,
                isLoading = false,
            ),
        )
    }
}

package net.thunderbird.feature.account.settings.impl.ui.general

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

internal class GeneralSettingsStateTest {

    @Test
    fun `should set default values`() {
        // Arrange
        val state = State()

        // Assert
        assertThat(state).isEqualTo(
            State(
                subtitle = null,
            ),
        )
    }
}

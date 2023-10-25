package app.k9mail.feature.account.edit.ui.server.settings.save

import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class SaveServerSettingsStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                error = null,
                isLoading = true,
            ),
        )
    }
}

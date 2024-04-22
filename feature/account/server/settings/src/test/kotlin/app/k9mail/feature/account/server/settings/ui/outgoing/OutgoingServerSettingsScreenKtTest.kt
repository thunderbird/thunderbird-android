package app.k9mail.feature.account.server.settings.ui.outgoing

import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OutgoingServerSettingsScreenKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeOutgoingServerSettingsViewModel(initialState = initialState)
        var onNextCounter = 0
        var onBackCounter = 0

        setContentWithTheme {
            OutgoingServerSettingsScreen(
                onNext = { onNextCounter++ },
                onBack = { onBackCounter++ },
                viewModel = viewModel,
            )
        }

        assertThat(onNextCounter).isEqualTo(0)
        assertThat(onBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateNext)

        assertThat(onNextCounter).isEqualTo(1)
        assertThat(onBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateBack)

        assertThat(onNextCounter).isEqualTo(1)
        assertThat(onBackCounter).isEqualTo(1)
    }
}

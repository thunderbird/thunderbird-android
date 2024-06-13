package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.account.setup.ui.FakeAppNameProvider
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncOptionsScreenKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeSyncOptionsViewModel(initialState)
        var onNextCounter = 0
        var onBackCounter = 0

        setContentWithTheme {
            SyncOptionsScreen(
                onNext = { onNextCounter++ },
                onBack = { onBackCounter++ },
                viewModel = viewModel,
                appNameProvider = FakeAppNameProvider,
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

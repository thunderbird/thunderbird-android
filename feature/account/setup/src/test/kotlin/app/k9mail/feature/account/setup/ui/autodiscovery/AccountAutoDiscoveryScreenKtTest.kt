package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.ui.FakeAppNameProvider
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountAutoDiscoveryScreenKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeAccountAutoDiscoveryViewModel(initialState)
        var onNextCounter = 0
        var onBackCounter = 0

        setContentWithTheme {
            AccountAutoDiscoveryScreen(
                onNext = { onNextCounter++ },
                onBack = { onBackCounter++ },
                viewModel = viewModel,
                appNameProvider = FakeAppNameProvider,
            )
        }

        assertThat(onNextCounter).isEqualTo(0)
        assertThat(onBackCounter).isEqualTo(0)

        viewModel.effect(
            Effect.NavigateNext(
                result = AccountAutoDiscoveryContract.AutoDiscoveryUiResult(
                    isAutomaticConfig = false,
                    incomingProtocolType = IncomingProtocolType.IMAP,
                ),
            ),
        )

        assertThat(onNextCounter).isEqualTo(1)
        assertThat(onBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateBack)

        assertThat(onNextCounter).isEqualTo(1)
        assertThat(onBackCounter).isEqualTo(1)
    }
}

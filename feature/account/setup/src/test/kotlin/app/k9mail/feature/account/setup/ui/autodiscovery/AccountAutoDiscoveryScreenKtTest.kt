package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithKoinAndTheme
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.ui.FakeBrandNameProvider
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.provider.BrandTypographyProvider
import org.junit.Test

class AccountAutoDiscoveryScreenKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeAccountAutoDiscoveryViewModel(initialState)
        var onNextCounter = 0
        var onBackCounter = 0
        var onImportAccountNavigateCounter = 0

        setContentWithKoinAndTheme(
            modules = {
                single<BrandTypographyProvider> { BrandTypographyProvider {} }
            },
        ) {
            AccountAutoDiscoveryScreen(
                onNext = { onNextCounter++ },
                onBack = { onBackCounter++ },
                onImportAccountNavigate = { onImportAccountNavigateCounter++ },
                viewModel = viewModel,
                brandNameProvider = FakeBrandNameProvider,
            )
        }

        assertThat(onNextCounter).isEqualTo(0)
        assertThat(onBackCounter).isEqualTo(0)
        assertThat(onImportAccountNavigateCounter).isEqualTo(0)

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
        assertThat(onImportAccountNavigateCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateBack)

        assertThat(onNextCounter).isEqualTo(1)
        assertThat(onBackCounter).isEqualTo(1)
        assertThat(onImportAccountNavigateCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateToImportAccount)
        assertThat(onNextCounter).isEqualTo(1)
        assertThat(onBackCounter).isEqualTo(1)
        assertThat(onImportAccountNavigateCounter).isEqualTo(1)
    }
}

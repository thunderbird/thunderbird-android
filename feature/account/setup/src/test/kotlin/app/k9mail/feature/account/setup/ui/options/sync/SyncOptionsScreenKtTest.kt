package app.k9mail.feature.account.setup.ui.options.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithKoinAndTheme
import app.k9mail.feature.account.setup.ui.FakeBrandNameProvider
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.provider.BrandTypographyProvider
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider
import org.junit.Test

class SyncOptionsScreenKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeSyncOptionsViewModel(initialState)
        var onNextCounter = 0
        var onBackCounter = 0

        setContentWithKoinAndTheme(
            modules = {
                single<BrandTypographyProvider> { BrandTypographyProvider {} }
                single { BrandBackgroundModifierProvider { Modifier } }
            },
        ) {
            SharedTransitionLayout {
                AnimatedVisibility(true) {
                    SyncOptionsScreen(
                        onNext = { onNextCounter++ },
                        onBack = { onBackCounter++ },
                        viewModel = viewModel,
                        brandNameProvider = FakeBrandNameProvider,
                        animatedVisibilityScope = this,
                    )
                }
            }
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

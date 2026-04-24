package app.k9mail.feature.account.server.validation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithKoinAndTheme
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Effect
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.common.provider.BrandTypographyProvider
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider
import org.junit.Test

class ServerValidationScreenKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeServerValidationViewModel(initialState = initialState)
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
                    ServerValidationScreen(
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

    private object FakeBrandNameProvider : BrandNameProvider {
        override val brandName: String = "K-9 Mail"
    }
}

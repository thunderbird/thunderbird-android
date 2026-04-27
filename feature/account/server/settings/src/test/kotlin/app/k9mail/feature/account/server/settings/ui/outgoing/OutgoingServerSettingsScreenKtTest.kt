package app.k9mail.feature.account.server.settings.ui.outgoing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithKoinAndTheme
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.common.provider.BrandTypographyProvider
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider
import org.junit.Test

class OutgoingServerSettingsScreenKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeOutgoingServerSettingsViewModel(initialState = initialState)
        var onNextCounter = 0
        var onBackCounter = 0

        setContentWithKoinAndTheme(
            modules = {
                single<BrandNameProvider> {
                    object : BrandNameProvider {
                        override val brandName: String = "Thunderbird"
                    }
                }
                single<BrandBackgroundModifierProvider> {
                    BrandBackgroundModifierProvider { Modifier }
                }
                single<BrandTypographyProvider> { BrandTypographyProvider {} }
            },
        ) {
            SharedTransitionLayout {
                AnimatedVisibility(true) {
                    OutgoingServerSettingsScreen(
                        onNext = { onNextCounter++ },
                        onBack = { onBackCounter++ },
                        viewModel = viewModel,
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

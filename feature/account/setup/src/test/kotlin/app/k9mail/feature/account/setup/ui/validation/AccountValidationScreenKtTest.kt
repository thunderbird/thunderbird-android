package app.k9mail.feature.account.setup.ui.validation

import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContent
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Effect
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountValidationScreenKtTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val initialState = State()
        val viewModel = FakeAccountValidationViewModel(initialState = initialState)
        var onNextCounter = 0
        var onBackCounter = 0

        setContent {
            ThunderbirdTheme {
                AccountValidationScreen(
                    onNext = { onNextCounter++ },
                    onBack = { onBackCounter++ },
                    viewModel = viewModel,
                )
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

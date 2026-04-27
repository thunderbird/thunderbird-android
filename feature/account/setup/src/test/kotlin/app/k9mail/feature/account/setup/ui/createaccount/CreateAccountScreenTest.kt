package app.k9mail.feature.account.setup.ui.createaccount

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithKoinAndTheme
import app.k9mail.feature.account.setup.domain.entity.AccountUuid
import app.k9mail.feature.account.setup.ui.FakeBrandNameProvider
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Effect
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.State
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.provider.BrandTypographyProvider
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider

class CreateAccountScreenTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val accountUuid = AccountUuid("irrelevant")
        val initialState = State(
            isLoading = false,
            error = null,
        )
        val viewModel = FakeCreateAccountViewModel(initialState)
        val navigateNextArguments = mutableListOf<AccountUuid>()
        var navigateBackCounter = 0

        setContentWithKoinAndTheme(
            modules = {
                single<BrandTypographyProvider> { BrandTypographyProvider {} }
                single { BrandBackgroundModifierProvider { Modifier } }
            },
        ) {
            SharedTransitionLayout {
                AnimatedVisibility(true) {
                    CreateAccountScreen(
                        onNext = { accountUuid -> navigateNextArguments.add(accountUuid) },
                        onBack = { navigateBackCounter++ },
                        viewModel = viewModel,
                        brandNameProvider = FakeBrandNameProvider,
                        animatedVisibilityScope = this,
                    )
                }
            }
        }

        assertThat(navigateNextArguments).isEmpty()
        assertThat(navigateBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateNext(accountUuid))

        assertThat(navigateNextArguments).containsExactly(accountUuid)
        assertThat(navigateBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateBack)

        assertThat(navigateNextArguments).containsExactly(accountUuid)
        assertThat(navigateBackCounter).isEqualTo(1)
    }
}

package app.k9mail.feature.account.setup.ui.createaccount

import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.account.setup.ui.FakeBrandNameProvider
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Effect
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.State
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory

class CreateAccountScreenTest : ComposeTest() {

    @Test
    fun `should delegate navigation effects`() = runTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            isLoading = false,
            error = null,
        )
        val viewModel = FakeCreateAccountViewModel(initialState)
        val navigateNextArguments = mutableListOf<AccountId>()
        var navigateBackCounter = 0

        setContentWithTheme {
            CreateAccountScreen(
                onNext = { accountUuid -> navigateNextArguments.add(accountUuid) },
                onBack = { navigateBackCounter++ },
                viewModel = viewModel,
                brandNameProvider = FakeBrandNameProvider,
            )
        }

        assertThat(navigateNextArguments).isEmpty()
        assertThat(navigateBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateNext(accountId))

        assertThat(navigateNextArguments).containsExactly(accountId)
        assertThat(navigateBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateBack)

        assertThat(navigateNextArguments).containsExactly(accountId)
        assertThat(navigateBackCounter).isEqualTo(1)
    }
}

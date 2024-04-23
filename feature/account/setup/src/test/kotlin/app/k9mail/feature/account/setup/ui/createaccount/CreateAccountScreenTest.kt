package app.k9mail.feature.account.setup.ui.createaccount

import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.account.setup.domain.entity.AccountUuid
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Effect
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.State
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

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

        setContentWithTheme {
            CreateAccountScreen(
                onNext = { accountUuid -> navigateNextArguments.add(accountUuid) },
                onBack = { navigateBackCounter++ },
                viewModel = viewModel,
            )
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

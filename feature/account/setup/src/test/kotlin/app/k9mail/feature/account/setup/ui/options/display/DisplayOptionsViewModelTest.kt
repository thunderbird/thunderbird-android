package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.input.StringInputField
import org.junit.Rule
import org.junit.Test

class DisplayOptionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountOwnerNameProvider = FakeAccountOwnerNameProvider()
    private val testSubject = DisplayOptionsViewModel(
        validator = FakeDisplayOptionsValidator(),
        accountStateRepository = InMemoryAccountStateRepository(),
        accountOwnerNameProvider = accountOwnerNameProvider,
    )

    @Test
    fun `should change state when OnAccountNameChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnAccountNameChanged("accountName"),
            expectedState = State(accountName = StringInputField(value = "accountName")),
        )
    }

    @Test
    fun `should change state when OnDisplayNameChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnDisplayNameChanged("displayName"),
            expectedState = State(displayName = StringInputField(value = "displayName")),
        )
    }

    @Test
    fun `should change state when OnEmailSignatureChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnEmailSignatureChanged("emailSignature"),
            expectedState = State(emailSignature = StringInputField(value = "emailSignature")),
        )
    }

    @Test
    fun `should change state and emit NavigateNext effect when OnNextClicked event received and input valid`() =
        runMviTest {
            val viewModel = testSubject
            val turbines = turbinesWithInitialStateCheck(viewModel, State())

            viewModel.event(Event.OnNextClicked)

            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(
                State(
                    accountName = StringInputField(value = "", isValid = true),
                    displayName = StringInputField(value = "", isValid = true),
                    emailSignature = StringInputField(value = "", isValid = true),
                ),
            )

            assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(Effect.NavigateNext)
        }

    @Test
    fun `should change state and not emit effect when OnNextClicked event received and input invalid`() =
        runMviTest {
            val viewModel = DisplayOptionsViewModel(
                validator = FakeDisplayOptionsValidator(
                    accountNameAnswer = Outcome.Failure(TestError),
                ),
                accountStateRepository = InMemoryAccountStateRepository(),
                accountOwnerNameProvider = accountOwnerNameProvider,
            )
            val turbines = turbinesWithInitialStateCheck(viewModel, State())

            viewModel.event(Event.OnNextClicked)

            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(
                State(
                    accountName = StringInputField(value = "", error = TestError, isValid = false),
                    displayName = StringInputField(value = "", isValid = true),
                    emailSignature = StringInputField(value = "", isValid = true),
                ),
            )
        }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runMviTest {
        val viewModel = testSubject
        val turbines = turbinesWithInitialStateCheck(viewModel, State())

        viewModel.event(Event.OnBackClicked)

        assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(Effect.NavigateBack)
    }

    @Test
    fun `should set owner name when LoadAccountState event received`() = runMviTest {
        accountOwnerNameProvider.ownerName = "Alice Example"
        val viewModel = testSubject
        val turbines = turbinesWithInitialStateCheck(viewModel, State())

        viewModel.event(Event.LoadAccountState)

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(
            State(displayName = StringInputField("Alice Example")),
        )
    }

    private object TestError : ValidationError
}

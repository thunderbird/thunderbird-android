package app.k9mail.feature.account.setup.ui.autoconfig

import app.cash.turbine.testIn
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.testing.eventStateTest
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Effect
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Event
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountAutoConfigViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testSubject = AccountAutoConfigViewModel(
        validator = FakeAccountAutoConfigValidator(),
    )

    @Test
    fun `should reset state when EmailAddressChanged event is received`() = runTest {
        val initialState = State(
            configStep = ConfigStep.PASSWORD,
            emailAddress = StringInputField(value = "email"),
            password = StringInputField(value = "password"),
        )
        testSubject.initState(initialState)

        eventStateTest(
            viewModel = testSubject,
            initialState = initialState,
            event = Event.EmailAddressChanged("email"),
            expectedState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(value = "email"),
                password = StringInputField(),
            ),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when PasswordChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.PasswordChanged("password"),
            expectedState = State(
                password = StringInputField(value = "password"),
            ),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change config step to password when OnNextClicked event is received`() = runTest {
        val initialState = State(
            configStep = ConfigStep.EMAIL_ADDRESS,
            emailAddress = StringInputField(value = "email"),
        )
        testSubject.initState(initialState)

        eventStateTest(
            viewModel = testSubject,
            initialState = initialState,
            event = Event.OnNextClicked,
            expectedState = State(
                configStep = ConfigStep.PASSWORD,
                emailAddress = StringInputField(
                    value = "email",
                    error = null,
                    isValid = true,
                ),
            ),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should not change config step to password when OnNextClicked event is received and input invalid`() = runTest {
        val initialState = State(
            configStep = ConfigStep.EMAIL_ADDRESS,
            emailAddress = StringInputField(value = "invalid email"),
        )
        val viewModel = AccountAutoConfigViewModel(
            validator = FakeAccountAutoConfigValidator(
                emailAddressAnswer = ValidationResult.Failure(TestError),
            ),
            initialState = initialState,
        )

        eventStateTest(
            viewModel = viewModel,
            initialState = initialState,
            event = Event.OnNextClicked,
            expectedState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(
                    value = "invalid email",
                    error = TestError,
                    isValid = false,
                ),
            ),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should emit NavigateNext when OnNextClicked received in password step with valid input`() =
        runTest {
            val initialState = State(
                configStep = ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "email"),
                password = StringInputField(value = "password"),
            )
            testSubject.initState(initialState)
            val stateTurbine = testSubject.state.testIn(backgroundScope)
            val effectTurbine = testSubject.effect.testIn(backgroundScope)
            val turbines = listOf(stateTurbine, effectTurbine)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(initialState)
            }

            testSubject.event(Event.OnNextClicked)

            assertThat(stateTurbine.awaitItem()).isEqualTo(
                State(
                    configStep = ConfigStep.PASSWORD,
                    emailAddress = StringInputField(
                        value = "email",
                        error = null,
                        isValid = true,
                    ),
                    password = StringInputField(
                        value = "password",
                        error = null,
                        isValid = true,
                    ),
                ),
            )

            assertThatAndTurbinesConsumed(
                actual = effectTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(Effect.NavigateNext)
            }
        }

    @Test
    fun `should not emit NavigateNext when OnNextClicked received in password step with invalid input`() =
        runTest {
            val initialState = State(
                configStep = ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "email"),
                password = StringInputField(value = "password"),
            )
            val viewModel = AccountAutoConfigViewModel(
                validator = FakeAccountAutoConfigValidator(
                    passwordAnswer = ValidationResult.Failure(TestError),
                ),
                initialState = initialState,
            )
            val stateTurbine = viewModel.state.testIn(backgroundScope)
            val effectTurbine = viewModel.effect.testIn(backgroundScope)
            val turbines = listOf(stateTurbine, effectTurbine)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(initialState)
            }

            viewModel.event(Event.OnNextClicked)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(
                    State(
                        configStep = ConfigStep.PASSWORD,
                        emailAddress = StringInputField(
                            value = "email",
                            error = null,
                            isValid = true,
                        ),
                        password = StringInputField(
                            value = "password",
                            error = TestError,
                            isValid = false,
                        ),
                    ),
                )
            }
        }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event is received`() = runTest {
        val viewModel = testSubject
        val stateTurbine = viewModel.state.testIn(backgroundScope)
        val effectTurbine = viewModel.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(State())
        }

        viewModel.event(Event.OnBackClicked)

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }

    @Test
    fun `should change config step to email address when OnBackClicked event is received in password config step`() =
        runTest {
            val initialState = State(
                configStep = ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "email"),
                password = StringInputField(value = "password"),
            )
            val viewModel = testSubject
            viewModel.initState(initialState)
            val stateTurbine = viewModel.state.testIn(backgroundScope)
            val effectTurbine = viewModel.effect.testIn(backgroundScope)
            val turbines = listOf(stateTurbine, effectTurbine)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(initialState)
            }

            viewModel.event(Event.OnBackClicked)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(
                    State(
                        configStep = ConfigStep.EMAIL_ADDRESS,
                        emailAddress = StringInputField(value = "email"),
                    ),
                )
            }
        }

    private object TestError : ValidationError
}

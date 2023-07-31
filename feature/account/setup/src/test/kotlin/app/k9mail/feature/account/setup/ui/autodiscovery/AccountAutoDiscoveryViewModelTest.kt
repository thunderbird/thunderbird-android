package app.k9mail.feature.account.setup.ui.autodiscovery

import app.cash.turbine.testIn
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.feature.account.setup.domain.entity.AutoDiscoverySettingsFixture
import app.k9mail.feature.account.setup.domain.input.BooleanInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Error
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AccountAutoDiscoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testSubject = AccountAutoDiscoveryViewModel(
        validator = FakeAccountAutoDiscoveryValidator(),
        getAutoDiscovery = {
            delay(50)
            AutoDiscoveryResult.NoUsableSettingsFound
        },
        oAuthViewModel = FakeAccountOAuthViewModel(),
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
    fun `should change state when ConfigurationApprovalChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.ConfigurationApprovalChanged(true),
            expectedState = State(
                configurationApproved = BooleanInputField(value = true),
            ),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state to password when OnNextClicked event is received, input valid and discovery loaded`() =
        runTest {
            val autoDiscoverySettings = AutoDiscoverySettingsFixture.settings
            val initialState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(value = "email"),
            )
            val viewModel = AccountAutoDiscoveryViewModel(
                validator = FakeAccountAutoDiscoveryValidator(),
                getAutoDiscovery = {
                    delay(50)
                    autoDiscoverySettings
                },
                oAuthViewModel = FakeAccountOAuthViewModel(),
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

            val validatedState = initialState.copy(
                emailAddress = StringInputField(
                    value = "email",
                    error = null,
                    isValid = true,
                ),
            )
            assertThat(stateTurbine.awaitItem()).isEqualTo(validatedState)

            val loadingState = validatedState.copy(
                isLoading = true,
            )
            assertThat(stateTurbine.awaitItem()).isEqualTo(loadingState)

            val successState = validatedState.copy(
                autoDiscoverySettings = autoDiscoverySettings,
                configStep = ConfigStep.PASSWORD,
                isLoading = false,
            )
            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(successState)
            }
        }

    @Test
    fun `should not change state when OnNextClicked event is received, input valid but discovery failed`() =
        runTest {
            val initialState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(value = "email"),
            )
            val discoveryError = Exception("discovery error")
            val viewModel = AccountAutoDiscoveryViewModel(
                validator = FakeAccountAutoDiscoveryValidator(),
                getAutoDiscovery = {
                    delay(50)
                    AutoDiscoveryResult.UnexpectedException(discoveryError)
                },
                oAuthViewModel = FakeAccountOAuthViewModel(),
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

            val validatedState = initialState.copy(
                emailAddress = StringInputField(
                    value = "email",
                    error = null,
                    isValid = true,
                ),
            )
            assertThat(stateTurbine.awaitItem()).isEqualTo(validatedState)

            val loadingState = validatedState.copy(
                isLoading = true,
            )
            assertThat(stateTurbine.awaitItem()).isEqualTo(loadingState)

            val failureState = validatedState.copy(
                isLoading = false,
                error = AccountAutoDiscoveryContract.Error.UnknownError,
            )
            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(failureState)
            }
        }

    @Test
    fun `should reset error state and change to password step when OnNextClicked event received when having error`() =
        runTest {
            val initialState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(
                    value = "email",
                    isValid = true,
                ),
                error = Error.UnknownError,
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
                        isValid = true,
                    ),
                    error = null,
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
        val viewModel = AccountAutoDiscoveryViewModel(
            validator = FakeAccountAutoDiscoveryValidator(
                emailAddressAnswer = ValidationResult.Failure(TestError),
            ),
            getAutoDiscovery = { AutoDiscoveryResult.NoUsableSettingsFound },
            oAuthViewModel = FakeAccountOAuthViewModel(),
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
                    configurationApproved = BooleanInputField(
                        value = null,
                        error = null,
                        isValid = true,
                    ),
                ),
            )

            assertThatAndTurbinesConsumed(
                actual = effectTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(Effect.NavigateNext(isAutomaticConfig = false))
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
            val viewModel = AccountAutoDiscoveryViewModel(
                validator = FakeAccountAutoDiscoveryValidator(
                    passwordAnswer = ValidationResult.Failure(TestError),
                ),
                getAutoDiscovery = { AutoDiscoveryResult.NoUsableSettingsFound },
                oAuthViewModel = FakeAccountOAuthViewModel(),
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
                        configurationApproved = BooleanInputField(
                            value = null,
                            error = null,
                            isValid = true,
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

    @Test
    fun `should reset error state when OnBackClicked event received when having error and in email address step`() =
        runTest {
            val initialState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(
                    value = "email",
                    isValid = true,
                ),
                error = Error.UnknownError,
            )
            testSubject.initState(initialState)

            eventStateTest(
                viewModel = testSubject,
                initialState = initialState,
                event = Event.OnBackClicked,
                expectedState = State(
                    configStep = ConfigStep.EMAIL_ADDRESS,
                    emailAddress = StringInputField(
                        value = "email",
                        isValid = true,
                    ),
                    error = null,
                ),
                coroutineScope = backgroundScope,
            )
        }

    @Test
    fun `should emit NavigateNext effect when OnEditConfigurationClicked event is received`() = runTest {
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

        viewModel.event(Event.OnEditConfigurationClicked)

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext(isAutomaticConfig = false))
        }
    }

    private object TestError : ValidationError
}

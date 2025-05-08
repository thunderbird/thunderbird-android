package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.assertThatAndMviTurbinesConsumed
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.input.BooleanInputField
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.oauth.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.setup.domain.entity.AutoDiscoverySettingsFixture
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.AutoDiscoveryUiResult
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Error
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.delay
import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult
import org.junit.Rule
import org.junit.Test

class AccountAutoDiscoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should reset state when EmailAddressChanged event is received`() = runMviTest {
        val initialState = State(
            configStep = ConfigStep.PASSWORD,
            emailAddress = StringInputField(value = "email"),
            password = StringInputField(value = "password"),
        )
        val testSubject = createTestSubject(initialState)

        eventStateTest(
            viewModel = testSubject,
            initialState = initialState,
            event = Event.EmailAddressChanged("email"),
            expectedState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(value = "email"),
                password = StringInputField(),
            ),
        )
    }

    @Test
    fun `should change state when PasswordChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = createTestSubject(),
            initialState = State(),
            event = Event.PasswordChanged("password"),
            expectedState = State(
                password = StringInputField(value = "password"),
            ),
        )
    }

    @Test
    fun `should change state when ResultApprovalChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = createTestSubject(),
            initialState = State(),
            event = Event.ResultApprovalChanged(true),
            expectedState = State(
                configurationApproved = BooleanInputField(value = true),
            ),
        )
    }

    @Test
    fun `should change state to password when OnNextClicked event is received, input valid and discovery loaded`() =
        runMviTest {
            val autoDiscoverySettings = AutoDiscoverySettingsFixture.settings
            val initialState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(value = "email"),
            )
            val testSubject = AccountAutoDiscoveryViewModel(
                validator = FakeAccountAutoDiscoveryValidator(),
                getAutoDiscovery = {
                    delay(50)
                    autoDiscoverySettings
                },
                oAuthViewModel = FakeAccountOAuthViewModel(),
                accountStateRepository = InMemoryAccountStateRepository(),
                initialState = initialState,
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            testSubject.event(Event.OnNextClicked)

            val validatedState = initialState.copy(
                emailAddress = StringInputField(
                    value = "email",
                    error = null,
                    isValid = true,
                ),
            )
            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(validatedState)

            val loadingState = validatedState.copy(
                isLoading = true,
            )
            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(loadingState)

            val successState = validatedState.copy(
                autoDiscoverySettings = autoDiscoverySettings,
                configStep = ConfigStep.PASSWORD,
                isLoading = false,
            )
            assertThatAndMviTurbinesConsumed(
                actual = turbines.stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(successState)
            }
        }

    @Test
    fun `should not change state when OnNextClicked event is received, input valid but discovery failed`() =
        runMviTest {
            val initialState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(value = "email"),
            )
            val discoveryError = Exception("discovery error")
            val testSubject = AccountAutoDiscoveryViewModel(
                validator = FakeAccountAutoDiscoveryValidator(),
                getAutoDiscovery = {
                    delay(50)
                    AutoDiscoveryResult.UnexpectedException(discoveryError)
                },
                oAuthViewModel = FakeAccountOAuthViewModel(),
                accountStateRepository = InMemoryAccountStateRepository(),
                initialState = initialState,
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            testSubject.event(Event.OnNextClicked)

            val validatedState = initialState.copy(
                emailAddress = StringInputField(
                    value = "email",
                    error = null,
                    isValid = true,
                ),
            )
            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(validatedState)

            val loadingState = validatedState.copy(
                isLoading = true,
            )
            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(loadingState)

            val failureState = validatedState.copy(
                isLoading = false,
                error = Error.UnknownError,
            )
            assertThatAndMviTurbinesConsumed(
                actual = turbines.stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(failureState)
            }
        }

    @Test
    fun `should reset error state and change to password step when OnNextClicked event received when having error`() =
        runMviTest {
            val initialState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(
                    value = "email",
                    isValid = true,
                ),
                error = Error.UnknownError,
            )
            val testSubject = createTestSubject(initialState)

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
            )
        }

    @Test
    fun `should not change config step to password when OnNextClicked event is received and input invalid`() =
        runMviTest {
            val initialState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(value = "invalid email"),
            )
            val testSubject = AccountAutoDiscoveryViewModel(
                validator = FakeAccountAutoDiscoveryValidator(
                    emailAddressAnswer = ValidationResult.Failure(TestError),
                ),
                getAutoDiscovery = { AutoDiscoveryResult.NoUsableSettingsFound },
                oAuthViewModel = FakeAccountOAuthViewModel(),
                accountStateRepository = InMemoryAccountStateRepository(),
                initialState = initialState,
            )

            eventStateTest(
                viewModel = testSubject,
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
            )
        }

    @Test
    fun `should save state and emit NavigateNext when OnNextClicked received in password step with valid input`() =
        runMviTest {
            val initialState = State(
                configStep = ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "email"),
                password = StringInputField(value = "password"),
            )
            val repository = InMemoryAccountStateRepository()
            val testSubject = createTestSubject(
                initialState = initialState,
                repository = repository,
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            testSubject.event(Event.OnNextClicked)

            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(
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

            assertThatAndMviTurbinesConsumed(
                actual = turbines.effectTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(
                    Effect.NavigateNext(
                        result = AutoDiscoveryUiResult(
                            isAutomaticConfig = false,
                            incomingProtocolType = null,
                        ),
                    ),
                )
            }

            assertThat(repository.getState()).isEqualTo(
                AccountState(
                    emailAddress = "email",
                    incomingServerSettings = null,
                    outgoingServerSettings = null,
                    authorizationState = null,
                    displayOptions = null,
                    syncOptions = null,
                ),
            )
        }

    @Test
    fun `should not emit NavigateNext when OnNextClicked received in password step with invalid input`() =
        runMviTest {
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
                accountStateRepository = InMemoryAccountStateRepository(),
                initialState = initialState,
            )
            val turbines = turbinesWithInitialStateCheck(viewModel, initialState)

            viewModel.event(Event.OnNextClicked)

            assertThatAndMviTurbinesConsumed(
                actual = turbines.stateTurbine.awaitItem(),
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
    fun `should emit NavigateBack effect when OnBackClicked event is received`() = runMviTest {
        val testSubject = createTestSubject()
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.OnBackClicked)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }

    @Test
    fun `should change config step to email address when OnBackClicked event is received in password config step`() =
        runMviTest {
            val initialState = State(
                configStep = ConfigStep.PASSWORD,
                emailAddress = StringInputField(value = "email"),
                password = StringInputField(value = "password"),
            )
            val testSubject = createTestSubject(initialState)
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            testSubject.event(Event.OnBackClicked)

            assertThatAndMviTurbinesConsumed(
                actual = turbines.stateTurbine.awaitItem(),
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
        runMviTest {
            val initialState = State(
                configStep = ConfigStep.EMAIL_ADDRESS,
                emailAddress = StringInputField(
                    value = "email",
                    isValid = true,
                ),
                error = Error.UnknownError,
            )
            val testSubject = createTestSubject(initialState)

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
            )
        }

    @Test
    fun `should emit NavigateNext effect when OnEditConfigurationClicked event is received`() = runMviTest {
        val initialState = State(
            autoDiscoverySettings = AutoDiscoverySettingsFixture.settings,
        )
        val testSubject = createTestSubject()
        testSubject.initState(initialState)
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnEditConfigurationClicked)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(
                Effect.NavigateNext(
                    result = AutoDiscoveryUiResult(
                        isAutomaticConfig = false,
                        incomingProtocolType = IncomingProtocolType.IMAP,
                    ),
                ),
            )
        }
    }

    private object TestError : ValidationError

    private companion object {
        fun createTestSubject(
            initialState: State = State(),
            repository: AccountDomainContract.AccountStateRepository = InMemoryAccountStateRepository(),
        ): AccountAutoDiscoveryViewModel {
            return AccountAutoDiscoveryViewModel(
                validator = FakeAccountAutoDiscoveryValidator(),
                getAutoDiscovery = {
                    delay(50)
                    AutoDiscoveryResult.NoUsableSettingsFound
                },
                accountStateRepository = repository,
                oAuthViewModel = FakeAccountOAuthViewModel(),
                initialState = initialState,
            )
        }
    }
}

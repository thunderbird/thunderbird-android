package app.k9mail.feature.account.setup.ui.createaccount

import app.cash.turbine.testIn
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.domain.entity.AccountUuid
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Effect
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Event
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.State
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class CreateAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeCreateAccount = FakeCreateAccount()
    private val accountStateRepository = InMemoryAccountStateRepository().apply {
        setState(ACCOUNT_STATE)
    }
    private val createAccountViewModel = CreateAccountViewModel(
        createAccount = fakeCreateAccount,
        accountStateRepository = accountStateRepository,
    )

    @Test
    fun `initial state should be loading state`() {
        assertThat(createAccountViewModel.state.value).isEqualTo(State(isLoading = true, error = null))
    }

    @Test
    fun `should change state and emit navigate effect after successfully creating account`() = runTest {
        val accountUuid = "accountUuid"
        fakeCreateAccount.result = AccountCreatorResult.Success(accountUuid)

        eventStateTest(
            viewModel = createAccountViewModel,
            initialState = State(isLoading = true, error = null),
            event = Event.CreateAccount,
            expectedState = State(isLoading = false, error = null),
            coroutineScope = backgroundScope,
        )

        assertThat(fakeCreateAccount.recordedInvocations).containsExactly(
            CreateAccountArguments(
                emailAddress = EMAIL_ADDRESS,
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AUTHORIZATION_STATE.state,
                options = ACCOUNT_OPTIONS,
            ),
        )

        val effectTurbine = createAccountViewModel.effect.testIn(backgroundScope)
        assertThat(effectTurbine.awaitItem()).isEqualTo(Effect.NavigateNext(AccountUuid(accountUuid)))
    }

    @Test
    fun `should change state when creating account has failed`() = runTest {
        val errorResult = AccountCreatorResult.Error("something went wrong")
        fakeCreateAccount.result = errorResult

        eventStateTest(
            viewModel = createAccountViewModel,
            initialState = State(isLoading = true, error = null),
            event = Event.CreateAccount,
            expectedState = State(isLoading = false, error = errorResult),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should ignore OnBackClicked event when in loading state`() = runTest {
        val effectTurbine = createAccountViewModel.effect.testIn(scope = backgroundScope)

        createAccountViewModel.event(Event.OnBackClicked)

        effectTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event was received while in success state`() = runTest {
        fakeCreateAccount.result = AccountCreatorResult.Success("accountUuid")
        createAccountViewModel.event(Event.CreateAccount)
        val effectTurbine = createAccountViewModel.effect.testIn(backgroundScope)

        createAccountViewModel.event(Event.OnBackClicked)

        assertThat(effectTurbine.awaitItem()).isEqualTo(Effect.NavigateBack)
    }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event was received while in error state`() = runTest {
        fakeCreateAccount.result = AccountCreatorResult.Error("something went wrong")
        createAccountViewModel.event(Event.CreateAccount)
        val effectTurbine = createAccountViewModel.effect.testIn(backgroundScope)

        createAccountViewModel.event(Event.OnBackClicked)

        assertThat(effectTurbine.awaitItem()).isEqualTo(Effect.NavigateBack)
    }

    companion object {
        private const val EMAIL_ADDRESS = "test@domain.example"

        private val INCOMING_SERVER_SETTINGS = ServerSettings(
            "imap",
            "imap.domain.example",
            993,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            AuthType.PLAIN,
            "username",
            "password",
            null,
        )

        private val OUTGOING_SERVER_SETTINGS = ServerSettings(
            "smtp",
            "smtp.domain.example",
            465,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            AuthType.PLAIN,
            "username",
            "password",
            null,
        )

        private val AUTHORIZATION_STATE = AuthorizationState("authorization state")

        private val ACCOUNT_OPTIONS = AccountOptions(
            accountName = "account name",
            displayName = "display name",
            emailSignature = null,
            checkFrequencyInMinutes = 0,
            messageDisplayCount = 50,
            showNotification = false,
        )

        private val ACCOUNT_STATE = AccountState(
            emailAddress = EMAIL_ADDRESS,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
            authorizationState = AUTHORIZATION_STATE,
            options = ACCOUNT_OPTIONS,
        )
    }
}

package app.k9mail.feature.account.setup.ui

import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.assertThatAndMviTurbinesConsumed
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@Suppress("LongMethod")
class AccountSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should forward step state on next event`() = runTest {
        var createAccountEmailAddress: String? = null
        var createAccountIncomingServerSettings: ServerSettings? = null
        var createAccountOutgoingServerSettings: ServerSettings? = null
        var createAccountAuthorizationState: String? = null
        var createAccountOptions: AccountOptions? = null
        val accountStateRepository = InMemoryAccountStateRepository()
        val viewModel = AccountSetupViewModel(
            createAccount = { emailAddress, incomingServerSettings, outgoingServerSettings, authState, options ->
                createAccountEmailAddress = emailAddress
                createAccountIncomingServerSettings = incomingServerSettings
                createAccountOutgoingServerSettings = outgoingServerSettings
                createAccountAuthorizationState = authState
                createAccountOptions = options

                "accountUuid"
            },
            accountStateRepository = accountStateRepository,
        )
        val turbines = turbinesWithInitialStateCheck(viewModel, State(setupStep = SetupStep.AUTO_CONFIG))

        viewModel.event(
            AccountSetupContract.Event.OnAutoDiscoveryFinished(
                isAutomaticConfig = false,
            ),
        )

        val expectedAccountState = AccountState(
            emailAddress = "test@domain.example",
            incomingServerSettings = ServerSettings(
                type = "imap",
                host = INCOMING_SERVER_NAME,
                port = INCOMING_SERVER_PORT,
                connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.CRAM_MD5,
                username = USERNAME,
                password = PASSWORD,
                clientCertificateAlias = null,
                extra = emptyMap(),
            ),
            outgoingServerSettings = ServerSettings(
                type = "smtp",
                host = OUTGOING_SERVER_NAME,
                port = OUTGOING_SERVER_PORT,
                connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.CRAM_MD5,
                username = USERNAME,
                password = PASSWORD,
                clientCertificateAlias = null,
                extra = emptyMap(),
            ),
            authorizationState = null,
            options = AccountOptions(
                accountName = "account name",
                displayName = "display name",
                emailSignature = "signature",
                checkFrequencyInMinutes = 15,
                messageDisplayCount = 25,
                showNotification = true,
            ),
        )

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.INCOMING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.INCOMING_VALIDATION)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OUTGOING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OUTGOING_VALIDATION)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OPTIONS)
        }

        accountStateRepository.setState(expectedAccountState)

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext("accountUuid"))
        }

        assertThat(createAccountEmailAddress).isEqualTo(EMAIL_ADDRESS)
        assertThat(createAccountIncomingServerSettings).isEqualTo(expectedAccountState.incomingServerSettings)
        assertThat(createAccountOutgoingServerSettings).isEqualTo(expectedAccountState.outgoingServerSettings)
        assertThat(createAccountAuthorizationState).isNull()
        assertThat(createAccountOptions).isEqualTo(expectedAccountState.options)
    }

    @Test
    fun `should rewind step state on back event`() = runTest {
        val initialState = State(setupStep = SetupStep.OPTIONS)
        val viewModel = AccountSetupViewModel(
            createAccount = { _, _, _, _, _ -> "accountUuid" },
            accountStateRepository = InMemoryAccountStateRepository(),
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(viewModel, initialState)

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OUTGOING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.INCOMING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.AUTO_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }

    @Test
    fun `should go back from OPTIONS step on back event when isAutomaticConfig enabled`() = runTest {
        val initialState = State(
            setupStep = SetupStep.OPTIONS,
            isAutomaticConfig = true,
        )
        val viewModel = AccountSetupViewModel(
            createAccount = { _, _, _, _, _ -> "accountUuid" },
            accountStateRepository = InMemoryAccountStateRepository(),
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(viewModel, initialState)

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.AUTO_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }

    @Test
    fun `should go back from OUTGOING_VALIDATION step state on back event when isAutomaticConfig enabled`() = runTest {
        val initialState = State(
            setupStep = SetupStep.OUTGOING_VALIDATION,
            isAutomaticConfig = true,
        )
        val viewModel = AccountSetupViewModel(
            createAccount = { _, _, _, _, _ -> "accountUuid" },
            accountStateRepository = InMemoryAccountStateRepository(),
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(viewModel, initialState)

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.AUTO_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }

    @Test
    fun `should go back from INCOMING_VALIDATION step state on back event when isAutomaticConfig enabled`() = runTest {
        val initialState = State(
            setupStep = SetupStep.OUTGOING_VALIDATION,
            isAutomaticConfig = true,
        )
        val viewModel = AccountSetupViewModel(
            createAccount = { _, _, _, _, _ -> "accountUuid" },
            accountStateRepository = InMemoryAccountStateRepository(),
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(viewModel, initialState)

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.AUTO_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }

    companion object {
        private const val EMAIL_ADDRESS = "test@domain.example"
        private const val USERNAME = EMAIL_ADDRESS
        private const val PASSWORD = "password"
        private const val INCOMING_SERVER_NAME = "imap.domain.example"
        private const val INCOMING_SERVER_PORT = 993
        private const val OUTGOING_SERVER_NAME = "smtp.domain.example"
        private const val OUTGOING_SERVER_PORT = 465
    }
}

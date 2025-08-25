package app.k9mail.feature.account.setup.ui.createaccount

import app.cash.turbine.turbineScope
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
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
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.folders.FolderServerId
import com.fsck.k9.mail.folders.RemoteFolder
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
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
    fun `should change state and emit navigate effect after successfully creating account`() = runMviTest {
        val accountUuid = "accountUuid"
        fakeCreateAccount.result = AccountCreatorResult.Success(accountUuid)
        val turbines = turbinesWithInitialStateCheck(createAccountViewModel, State(isLoading = true, error = null))

        createAccountViewModel.event(Event.CreateAccount)

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(State(isLoading = false, error = null))

        assertThat(fakeCreateAccount.recordedInvocations).containsExactly(
            AccountState(
                emailAddress = EMAIL_ADDRESS,
                incomingServerSettings = INCOMING_SERVER_SETTINGS,
                outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
                authorizationState = AUTHORIZATION_STATE,
                specialFolderSettings = SPECIAL_FOLDER_SETTINGS,
                displayOptions = ACCOUNT_DISPLAY_OPTIONS,
                syncOptions = ACCOUNT_SYNC_OPTIONS,
            ),
        )

        assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(Effect.NavigateNext(AccountUuid(accountUuid)))
    }

    @Test
    fun `should change state when creating account has failed`() = runMviTest {
        val errorResult = AccountCreatorResult.Error("something went wrong")
        fakeCreateAccount.result = errorResult

        eventStateTest(
            viewModel = createAccountViewModel,
            initialState = State(isLoading = true, error = null),
            event = Event.CreateAccount,
            expectedState = State(isLoading = false, error = errorResult),
        )
    }

    @Test
    fun `should ignore OnBackClicked event when in loading state`() = runTest {
        turbineScope {
            val effectTurbine = createAccountViewModel.effect.testIn(scope = backgroundScope)

            createAccountViewModel.event(Event.OnBackClicked)

            effectTurbine.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event was received while in success state`() = runTest {
        turbineScope {
            fakeCreateAccount.result = AccountCreatorResult.Success("accountUuid")
            createAccountViewModel.event(Event.CreateAccount)
            val effectTurbine = createAccountViewModel.effect.testIn(backgroundScope)

            createAccountViewModel.event(Event.OnBackClicked)

            assertThat(effectTurbine.awaitItem()).isEqualTo(Effect.NavigateBack)
        }
    }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event was received while in error state`() = runTest {
        turbineScope {
            fakeCreateAccount.result = AccountCreatorResult.Error("something went wrong")
            createAccountViewModel.event(Event.CreateAccount)
            val effectTurbine = createAccountViewModel.effect.testIn(backgroundScope)

            createAccountViewModel.event(Event.OnBackClicked)

            assertThat(effectTurbine.awaitItem()).isEqualTo(Effect.NavigateBack)
        }
    }

    private companion object {
        const val EMAIL_ADDRESS = "test@domain.example"

        val INCOMING_SERVER_SETTINGS = ServerSettings(
            "imap",
            "imap.domain.example",
            993,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            AuthType.PLAIN,
            "username",
            "password",
            null,
        )

        val OUTGOING_SERVER_SETTINGS = ServerSettings(
            "smtp",
            "smtp.domain.example",
            465,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            AuthType.PLAIN,
            "username",
            "password",
            null,
        )

        val AUTHORIZATION_STATE = AuthorizationState("authorization state")

        val SPECIAL_FOLDER_SETTINGS = SpecialFolderSettings(
            archiveSpecialFolderOption = SpecialFolderOption.Special(
                remoteFolder = RemoteFolder(
                    FolderServerId("archive folder"),
                    "archive folder",
                    FolderType.ARCHIVE,
                ),
            ),
            draftsSpecialFolderOption = SpecialFolderOption.Special(
                remoteFolder = RemoteFolder(
                    FolderServerId("drafts folder"),
                    "drafts folder",
                    FolderType.DRAFTS,
                ),
            ),
            sentSpecialFolderOption = SpecialFolderOption.Special(
                remoteFolder = RemoteFolder(
                    FolderServerId("sent folder"),
                    "sent folder",
                    FolderType.SENT,
                ),
            ),
            spamSpecialFolderOption = SpecialFolderOption.Special(
                remoteFolder = RemoteFolder(
                    FolderServerId("spam folder"),
                    "spam folder",
                    FolderType.SPAM,
                ),
            ),
            trashSpecialFolderOption = SpecialFolderOption.Special(
                remoteFolder = RemoteFolder(
                    FolderServerId("trash folder"),
                    "trash folder",
                    FolderType.TRASH,
                ),
            ),
        )

        val ACCOUNT_DISPLAY_OPTIONS = AccountDisplayOptions(
            accountName = "account name",
            displayName = "display name",
            emailSignature = null,
        )

        val ACCOUNT_SYNC_OPTIONS = AccountSyncOptions(
            checkFrequencyInMinutes = 0,
            messageDisplayCount = 50,
            showNotification = false,
        )

        val ACCOUNT_STATE = AccountState(
            emailAddress = EMAIL_ADDRESS,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
            authorizationState = AUTHORIZATION_STATE,
            specialFolderSettings = SPECIAL_FOLDER_SETTINGS,
            displayOptions = ACCOUNT_DISPLAY_OPTIONS,
            syncOptions = ACCOUNT_SYNC_OPTIONS,
        )
    }
}

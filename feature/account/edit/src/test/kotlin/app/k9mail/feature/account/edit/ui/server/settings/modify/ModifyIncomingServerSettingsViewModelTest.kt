package app.k9mail.feature.account.edit.ui.server.settings.modify

import app.k9mail.core.ui.compose.testing.mvi.assertThatAndMviTurbinesConsumed
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.account.AccountIdFactory

class ModifyIncomingServerSettingsViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mainDispatcher = MainDispatcherHelper(UnconfinedTestDispatcher())

    @BeforeTest
    fun setUp() {
        mainDispatcher.setUp()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
    }

    @Test
    fun `should load account state from use case`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val accountState = AccountState(
            id = accountId,
            emailAddress = "test@example.com",
            incomingServerSettings = ServerSettings(
                "imap",
                "imap.example.com",
                123,
                MailConnectionSecurity.SSL_TLS_REQUIRED,
                AuthType.PLAIN,
                "username",
                "password",
                clientCertificateAlias = null,
                extra = ImapStoreSettings.createExtra(
                    autoDetectNamespace = true,
                    pathPrefix = null,
                    useCompression = true,
                    sendClientInfo = true,
                ),
            ),
        )

        val testSubject = ModifyIncomingServerSettingsViewModel(
            accountId = accountId,
            accountStateLoader = { _ ->
                delay(50)
                accountState
            },
            validator = FakeIncomingServerSettingsValidator(),
            accountStateRepository = InMemoryAccountStateRepository(),
            oauthConfigurationProvider = { _ -> null },
            initialState = State(),
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.LoadAccountState)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.awaitStateItem(),
            turbines = turbines,
        ) {
            isEqualTo(
                State(
                    server = StringInputField(value = "imap.example.com"),
                    security = ConnectionSecurity.TLS,
                    port = NumberInputField(value = 123L),
                    authenticationType = AuthenticationType.PasswordCleartext,
                    username = StringInputField(value = "username"),
                    password = StringInputField(value = "password"),
                    imapAutodetectNamespaceEnabled = true,
                    imapPrefix = StringInputField(value = ""),
                    imapUseCompression = true,
                    imapSendClientInfo = true,
                ),
            )
        }
    }

    @Test
    fun `Graph account exposes reauthorization when its OAuth configuration is complete`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val accountState = AccountState(
            id = accountId,
            emailAddress = "test@example.com",
            incomingServerSettings = ServerSettings(
                type = "graph",
                host = "graph.microsoft.com",
                port = 443,
                connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.XOAUTH2,
                username = "test@example.com",
                password = null,
                clientCertificateAlias = null,
            ),
        )
        val testSubject = ModifyIncomingServerSettingsViewModel(
            accountId = accountId,
            accountStateLoader = { accountState },
            validator = FakeIncomingServerSettingsValidator(),
            accountStateRepository = InMemoryAccountStateRepository(),
            oauthConfigurationProvider = { hostname ->
                if (hostname == "graph.microsoft.com") {
                    OAuthConfiguration(
                        clientId = "client",
                        scopes = listOf("openid", "Mail.ReadWrite"),
                        authorizationEndpoint = "https://login.microsoftonline.com/authorize",
                        tokenEndpoint = "https://login.microsoftonline.com/token",
                        redirectUri = "tb://oauth",
                    )
                } else {
                    null
                }
            },
            initialState = State(),
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.LoadAccountState)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.awaitStateItem(),
            turbines = turbines,
        ) {
            isEqualTo(
                State(
                    protocolType = app.k9mail.feature.account.common.domain.entity.IncomingProtocolType.GRAPH,
                    server = StringInputField(value = "graph.microsoft.com"),
                    security = ConnectionSecurity.TLS,
                    port = NumberInputField(value = 443L),
                    authenticationType = AuthenticationType.OAuth2,
                    username = StringInputField(value = "test@example.com"),
                    password = StringInputField(value = ""),
                    isGraphReauthorizationAvailable = true,
                    preserveLegacyGraphProtocol = true,
                ),
            )
        }
    }
}

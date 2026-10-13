package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.core.validation.input.StringInputField

class NativeApiSettingsValidationTest {
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
    fun `native API setup requires a Client ID`() = runMviTest {
        val initialState = State(
            protocolType = IncomingProtocolType.GRAPH,
            apiOAuthRedirectUri = StringInputField(value = "$APP_ID:/oauth2redirect"),
        )
        val repository = InMemoryAccountStateRepository()
        val testSubject = IncomingServerSettingsViewModel(
            mode = InteractionMode.Create,
            validator = FakeIncomingServerSettingsValidator(),
            accountStateRepository = repository,
            applicationId = APP_ID,
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnNextClicked)

        assertThat(turbines.awaitStateItem().apiConfigurationError).isEqualTo(true)
        assertThat(repository.getState().incomingServerSettings).isEqualTo(null)
        turbines.effectTurbine.expectNoEvents()
    }

    @Test
    fun `native API setup rejects a callback URI not registered by the app`() = runMviTest {
        val initialState = State(
            protocolType = IncomingProtocolType.GRAPH,
            apiOAuthClientId = StringInputField(value = "account-client-id"),
            apiOAuthRedirectUri = StringInputField(value = "https://example.com/oauth2redirect"),
        )
        val repository = InMemoryAccountStateRepository()
        val testSubject = IncomingServerSettingsViewModel(
            mode = InteractionMode.Create,
            validator = FakeIncomingServerSettingsValidator(),
            accountStateRepository = repository,
            applicationId = APP_ID,
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnNextClicked)

        assertThat(turbines.awaitStateItem().apiConfigurationError).isEqualTo(true)
        assertThat(repository.getState().incomingServerSettings).isEqualTo(null)
        turbines.effectTurbine.expectNoEvents()
    }

    private companion object {
        const val APP_ID = "net.thunderbird.android.debug"
    }
}

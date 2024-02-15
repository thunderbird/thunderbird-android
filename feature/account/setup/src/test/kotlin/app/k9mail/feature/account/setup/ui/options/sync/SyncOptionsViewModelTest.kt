package app.k9mail.feature.account.setup.ui.options.sync

import app.cash.turbine.testIn
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.assertThatAndEffectTurbineConsumed
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SyncOptionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testSubject = SyncOptionsViewModel(
        accountStateRepository = InMemoryAccountStateRepository(),
    )

    @Test
    fun `should change state when OnCheckFrequencyChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnCheckFrequencyChanged(EmailCheckFrequency.EVERY_12_HOURS),
            expectedState = State(checkFrequency = EmailCheckFrequency.EVERY_12_HOURS),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnMessageDisplayCountChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnMessageDisplayCountChanged(EmailDisplayCount.MESSAGES_1000),
            expectedState = State(messageDisplayCount = EmailDisplayCount.MESSAGES_1000),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnShowNotificationChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnShowNotificationChanged(false),
            expectedState = State(showNotification = false),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should store state and emit NavigateNext effect when OnNextClicked event received and input valid`() =
        runTest {
            val accountStateRepository = InMemoryAccountStateRepository()
            val initialState = State(
                checkFrequency = EmailCheckFrequency.EVERY_HOUR,
                messageDisplayCount = EmailDisplayCount.MESSAGES_1000,
                showNotification = true,
            )
            val viewModel = SyncOptionsViewModel(
                accountStateRepository = accountStateRepository,
                initialState = initialState,
            )
            val turbines = turbinesWithInitialStateCheck(
                viewModel = viewModel,
                initialState = initialState,
            )

            viewModel.event(Event.OnNextClicked)

            turbines.assertThatAndEffectTurbineConsumed {
                isEqualTo(Effect.NavigateNext)
            }

            assertThat(accountStateRepository.getState()).isEqualTo(
                AccountState(
                    syncOptions = AccountSyncOptions(
                        checkFrequencyInMinutes = 60,
                        messageDisplayCount = 1000,
                        showNotification = true,
                    ),
                ),
            )
        }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runTest {
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
}

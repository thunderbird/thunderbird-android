package app.k9mail.feature.account.edit.ui.server.settings.save

import app.k9mail.core.ui.compose.testing.mvi.assertThatAndEffectTurbineConsumed
import app.k9mail.core.ui.compose.testing.mvi.assertThatAndStateTurbineConsumed
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.Effect
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.Event
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.Failure
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory

class BaseSaveServerSettingsViewModelTest {

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
    fun `should save server settings when SaveServerSettings event received and emit NavigateNext`() = runMviTest {
        var recordedAccountId: AccountId? = null
        var recordedIsIncoming: Boolean? = null
        val testSubject = TestSaveServerSettingsViewModel(
            accountId = ACCOUNT_ID,
            saveServerSettings = { accountId, isIncoming ->
                recordedAccountId = accountId
                recordedIsIncoming = isIncoming
            },
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.SaveServerSettings)

        turbines.assertThatAndStateTurbineConsumed {
            isEqualTo(State(isLoading = false))
        }

        assertThat(recordedAccountId).isNotNull().isEqualTo(ACCOUNT_ID)
        assertThat(recordedIsIncoming).isNotNull().isEqualTo(true)

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.NavigateNext)
        }
    }

    @Test
    fun `should set error state when save settings failed`() = runMviTest {
        val testSubject = TestSaveServerSettingsViewModel(
            accountId = ACCOUNT_ID,
            saveServerSettings = { _, _ ->
                error("Test exception")
            },
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.SaveServerSettings)

        turbines.assertThatAndStateTurbineConsumed {
            isEqualTo(
                State(
                    error = Failure.SaveServerSettingsFailed("Test exception"),
                    isLoading = false,
                ),
            )
        }
    }

    @Test
    fun `should allow NavigateBack when error and not loading`() = runMviTest {
        val failure = Failure.SaveServerSettingsFailed("Test exception")
        val testSubject = TestSaveServerSettingsViewModel(
            accountId = ACCOUNT_ID,
            saveServerSettings = { _, _ ->
                // Do nothing
            },
            initialState = State(
                isLoading = false,
                error = failure,
            ),
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State(isLoading = false, error = failure))

        testSubject.event(Event.OnBackClicked)

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.NavigateBack)
        }
    }

    private class TestSaveServerSettingsViewModel(
        accountId: AccountId,
        saveServerSettings: AccountEditDomainContract.UseCase.SaveServerSettings,
        initialState: State = State(),
    ) : BaseSaveServerSettingsViewModel(
        accountId = accountId,
        isIncoming = true,
        saveServerSettings = saveServerSettings,
        initialState = initialState,
    )

    private companion object {
        val ACCOUNT_ID = AccountIdFactory.create()
    }
}

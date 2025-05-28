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
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class BaseSaveServerSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should save server settings when SaveServerSettings event received and emit NavigateNext`() = runMviTest {
        var recordedAccountUuid: String? = null
        var recordedIsIncoming: Boolean? = null
        val testSubject = TestSaveServerSettingsViewModel(
            accountUuid = ACCOUNT_UUID,
            saveServerSettings = { accountUuid, isIncoming ->
                recordedAccountUuid = accountUuid
                recordedIsIncoming = isIncoming
            },
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.SaveServerSettings)

        turbines.assertThatAndStateTurbineConsumed {
            isEqualTo(State(isLoading = false))
        }

        assertThat(recordedAccountUuid).isNotNull().isEqualTo(ACCOUNT_UUID)
        assertThat(recordedIsIncoming).isNotNull().isEqualTo(true)

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.NavigateNext)
        }
    }

    @Test
    fun `should set error state when save settings failed`() = runMviTest {
        val testSubject = TestSaveServerSettingsViewModel(
            accountUuid = ACCOUNT_UUID,
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
            accountUuid = ACCOUNT_UUID,
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
        accountUuid: String,
        saveServerSettings: AccountEditDomainContract.UseCase.SaveServerSettings,
        initialState: State = State(),
    ) : BaseSaveServerSettingsViewModel(
        accountUuid = accountUuid,
        isIncoming = true,
        saveServerSettings = saveServerSettings,
        initialState = initialState,
    )

    private companion object {
        const val ACCOUNT_UUID = "accountUuid"
    }
}

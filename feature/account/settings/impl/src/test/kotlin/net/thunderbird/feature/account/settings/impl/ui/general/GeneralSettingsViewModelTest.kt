package net.thunderbird.feature.account.settings.impl.ui.general

import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State
import org.junit.Rule

class GeneralSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should load general settings`() = runMviTest {
        val accountId = AccountId.create()
        val initialState = State(
            subtitle = "Subtitle",
            preferences = persistentListOf(),
        )
        val preferences = FakeData.preferences

        generalSettingsRobot(accountId, initialState, preferences) {
            verifyGeneralSettingsLoaded(preferences)
        }
    }

    @Test
    fun `should navigate back when back is pressed`() = runMviTest {
        val accountId = AccountId.create()
        val initialState = State(
            subtitle = "Subtitle",
            preferences = persistentListOf(),
        )
        val preferences = FakeData.preferences

        generalSettingsRobot(accountId, initialState, preferences) {
            pressBack()
            verifyBackNavigation()
        }
    }
}

private suspend fun MviContext.generalSettingsRobot(
    accountId: AccountId,
    initialState: State,
    preferences: ImmutableList<Preference>,
    interaction: suspend GeneralSettingsRobot.() -> Unit,
) = GeneralSettingsRobot(this, accountId, initialState, preferences).apply {
    initialize()
    interaction()
}

private class GeneralSettingsRobot(
    private val mviContext: MviContext,
    private val accountId: AccountId,
    private val initialState: State = State(),
    private val preferences: ImmutableList<Preference>,
) {
    private val viewModel: GeneralSettingsContract.ViewModel = GeneralSettingsViewModel(
        accountId = accountId,
        getGeneralPreferences = {
            flowOf(
                Outcome.Success(preferences),
            ).onEach { delay(100) }
        },
        initialState = initialState,
    )

    private lateinit var turbines: MviTurbines<State, Effect>

    suspend fun initialize() {
        turbines = mviContext.turbinesWithInitialStateCheck(
            initialState = initialState,
            viewModel = viewModel,
        )
    }

    suspend fun verifyGeneralSettingsLoaded(preferences: ImmutableList<Preference>) {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                preferences = preferences,
            ),
        )
    }

    fun pressBack() {
        viewModel.event(GeneralSettingsContract.Event.OnBackPressed)
    }

    suspend fun verifyBackNavigation() {
        assertThat(turbines.awaitEffectItem()).isEqualTo(
            Effect.NavigateBack,
        )
    }
}

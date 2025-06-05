package net.thunderbird.feature.account.settings.impl.ui.general

import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State
import org.junit.Before
import org.junit.Rule

class GeneralSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `should load account name`() = runMviTest {
        val accountId = AccountId.create()
        val initialState = State(
            subtitle = null,
            preferences = persistentListOf(),
        )

        generalSettingsRobot(accountId, initialState, persistentListOf()) {
            verifyAccountNameLoaded()
        }
    }

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
            verifyGeneralSettingsLoaded(preferences)
            pressBack()
            verifyBackNavigation()
        }
    }

    @Test
    fun `should update preference when changed`() = runMviTest {
        val accountId = AccountId.create()
        val initialState = State(
            subtitle = "Subtitle",
            preferences = persistentListOf(),
        )
        val preferences = FakeData.preferences

        generalSettingsRobot(accountId, initialState, preferences) {
            verifyGeneralSettingsLoaded(preferences)
            val updatedPreference = (preferences.first() as PreferenceSetting.Text).copy(
                title = { "Updated Title" },
                description = { "Updated Description" },
            )
            updatePreference(updatedPreference)

            verifyPreferenceUpdated(updatedPreference)
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
    private lateinit var preferencesState: MutableStateFlow<ImmutableList<Preference>>
    private lateinit var turbines: MviTurbines<State, Effect>

    private val viewModel: GeneralSettingsContract.ViewModel by lazy {
        GeneralSettingsViewModel(
            accountId = accountId,
            getAccountName = {
                flowOf(Outcome.success("Subtitle"))
            },
            getGeneralPreferences = {
                preferencesState.map {
                    println("Loading preferences: $it")
                    Outcome.success(it)
                }
            },
            updateGeneralPreferences = { _, preference ->
                preferencesState.value = preferencesState.value.map { existingPreference ->
                    if (existingPreference is PreferenceSetting<*> && existingPreference.id == preference.id) {
                        println("Updating preference: ${preference.id}")
                        println("Old preference: $existingPreference")
                        println("New preference: $preference")
                        preference
                    } else {
                        existingPreference
                    }
                }.toImmutableList()
                Outcome.success(Unit)
            },
            initialState = initialState,
        )
    }

    suspend fun initialize() {
        preferencesState = MutableStateFlow(preferences)

        turbines = mviContext.turbinesWithInitialStateCheck(
            initialState = initialState,
            viewModel = viewModel,
        )
    }

    suspend fun verifyAccountNameLoaded() {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                subtitle = "Subtitle",
            ),
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

    fun updatePreference(preference: PreferenceSetting<*>) {
        viewModel.event(GeneralSettingsContract.Event.OnPreferenceSettingChange(preference))
    }

    suspend fun verifyPreferenceUpdated(preference: PreferenceSetting<*>) {
        val updatedPreference = turbines.awaitStateItem().preferences
            .filterIsInstance<PreferenceSetting<*>>()
            .find { it.id == preference.id }

        assertThat(updatedPreference).isEqualTo(preference)
    }
}

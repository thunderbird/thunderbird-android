package net.thunderbird.feature.account.settings.impl.ui.general

import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
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
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.core.ui.setting.emptySettings
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.impl.domain.entity.GeneralPreference
import net.thunderbird.feature.account.settings.impl.domain.entity.generateId
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State
import org.junit.Before
import org.junit.Rule

private fun createSettings(accountId: AccountId): Settings = persistentListOf(
    SettingValue.Text(
        id = GeneralPreference.NAME.generateId(accountId),
        title = { "Title" },
        description = { "Description" },
        icon = { null },
        value = "Test",
    ),
)

class GeneralSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `should load account name`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = null,
            settings = emptySettings(),
        )

        generalSettingsRobot(accountId, initialState, persistentListOf()) {
            verifyAccountNameLoaded()
        }
    }

    @Test
    fun `should load general settings`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = "Subtitle",
            settings = emptySettings(),
        )
        val settings = FakeData.settings

        generalSettingsRobot(accountId, initialState, settings) {
            verifyGeneralSettingsLoaded(settings)
        }
    }

    @Test
    fun `should navigate back when back is pressed`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = "Subtitle",
            settings = emptySettings(),
        )
        val settings = FakeData.settings

        generalSettingsRobot(accountId, initialState, settings) {
            verifyGeneralSettingsLoaded(settings)
            pressBack()
            verifyBackNavigation()
        }
    }

    @Test
    fun `should update preference when changed`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = "Subtitle",
            settings = emptySettings(),
        )
        val settings = createSettings(accountId)

        generalSettingsRobot(accountId, initialState, settings) {
            verifyGeneralSettingsLoaded(settings)
            val updatedPreference = (settings.first() as SettingValue.Text).copy(
                title = { "Updated Title" },
                description = { "Updated Description" },
            )
            updateSetting(updatedPreference)

            verifySettingUpdated(updatedPreference)
        }
    }
}

private suspend fun MviContext.generalSettingsRobot(
    accountId: AccountId,
    initialState: State,
    settings: Settings,
    interaction: suspend GeneralSettingsRobot.() -> Unit,
) = GeneralSettingsRobot(this, accountId, initialState, settings).apply {
    initialize()
    interaction()
}

private class GeneralSettingsRobot(
    private val mviContext: MviContext,
    private val accountId: AccountId,
    private val initialState: State = State(),
    private val settings: Settings,
) {
    private lateinit var settingsState: MutableStateFlow<Settings>
    private lateinit var turbines: MviTurbines<State, Effect>
    private var lastSetting: SettingValue<*>? = null

    private val viewModel: GeneralSettingsContract.ViewModel by lazy {
        GeneralSettingsViewModel(
            accountId = accountId,
            getAccountName = {
                flowOf(Outcome.success("Subtitle"))
            },
            getGeneralSettings = {
                settingsState.map {
                    println("Loading preferences: $it")
                    Outcome.success(it)
                }
            },
            updateGeneralSettings = { _, _ ->
                val newSetting = lastSetting
                if (newSetting != null) {
                    settingsState.value = settingsState.value.map { existingSetting ->
                        if (existingSetting is SettingValue<*> && existingSetting.id == newSetting.id) {
                            println("Updating setting: ${newSetting.id}")
                            println("Old setting: $existingSetting")
                            println("New setting: $newSetting")
                            newSetting
                        } else {
                            existingSetting
                        }
                    }.toImmutableList()
                }
                Outcome.success(Unit)
            },
            initialState = initialState,
        )
    }

    suspend fun initialize() {
        settingsState = MutableStateFlow(settings)

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

    suspend fun verifyGeneralSettingsLoaded(settings: Settings) {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                settings = settings,
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

    fun updateSetting(setting: SettingValue<*>) {
        lastSetting = setting
        viewModel.event(GeneralSettingsContract.Event.OnSettingValueChange(setting))
    }

    suspend fun verifySettingUpdated(setting: SettingValue<*>) {
        val updatedSetting = turbines.awaitStateItem().settings
            .filterIsInstance<SettingValue<*>>()
            .find { it.id == setting.id }

        assertThat(updatedSetting).isEqualTo(setting)
    }
}

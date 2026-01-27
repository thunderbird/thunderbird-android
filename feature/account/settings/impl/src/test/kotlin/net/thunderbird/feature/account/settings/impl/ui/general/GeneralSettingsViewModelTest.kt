package net.thunderbird.feature.account.settings.impl.ui.general

import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.advanceUntilIdle
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import net.thunderbird.core.validation.input.IntegerInputField
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateGeneralSettingCommand
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
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = null,
        )

        generalSettingsRobot(accountId, initialState) {
            verifyAccountNameLoaded()
        }
    }

    @Test
    fun `should load general settings`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = "Subtitle",
        )
        val profile = AccountProfile(
            id = accountId,
            name = "John",
            color = 0xFF0000,
            avatar = Avatar.Monogram("J"),
        )

        generalSettingsRobot(accountId, initialState, profile) {
            verifyGeneralSettingsLoaded(profile)
        }
    }

    @Test
    fun `should navigate back when back is pressed`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = "Subtitle",
        )

        generalSettingsRobot(accountId, initialState) {
            pressBack()
            verifyBackNavigation()
        }
    }

    @Test
    fun `should send update command when name changed`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = "Subtitle",
        )

        generalSettingsRobot(accountId, initialState) {
            changeName("New Name")
            verifyLastCommand(UpdateGeneralSettingCommand.UpdateName("New Name"))
        }
    }

    @Test
    fun `should send update command when color changed`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = "Subtitle",
        )

        generalSettingsRobot(accountId, initialState) {
            val newColor = 0x00FF00
            changeColor(newColor)
            verifyLastCommand(UpdateGeneralSettingCommand.UpdateColor(newColor))
        }
    }

    @Test
    fun `should send update command when avatar changed`() = runMviTest {
        val accountId = AccountIdFactory.create()
        val initialState = State(
            subtitle = "Subtitle",
        )

        generalSettingsRobot(accountId, initialState) {
            val newAvatar = Avatar.Monogram("A")
            changeAvatar(newAvatar)
            verifyLastCommand(UpdateGeneralSettingCommand.UpdateAvatar(newAvatar))
        }
    }
}

private suspend fun MviContext.generalSettingsRobot(
    accountId: AccountId,
    initialState: State,
    profile: AccountProfile? = null,
    interaction: suspend GeneralSettingsRobot.() -> Unit,
) = GeneralSettingsRobot(this, accountId, initialState, profile).apply {
    initialize()
    interaction()
}

private class GeneralSettingsRobot(
    private val mviContext: MviContext,
    private val accountId: AccountId,
    private val initialState: State = State(),
    private val initialProfile: AccountProfile? = null,
) {
    private lateinit var profileState: MutableStateFlow<AccountProfile?>
    private lateinit var turbines: MviTurbines<State, Effect>
    private var lastCommand: UpdateGeneralSettingCommand? = null

    private val viewModel: GeneralSettingsContract.ViewModel by lazy {
        GeneralSettingsViewModel(
            accountId = accountId,
            getAccountName = {
                flowOf(Outcome.success("Subtitle"))
            },
            getAccountProfile = {
                profileState.map { profile ->
                    profile?.let { Outcome.success(it) }
                        ?: Outcome.failure(
                            AccountSettingError.NotFound(
                                message = "Profile not found",
                            ),
                        )
                }
            },
            updateGeneralSettings = { _, command ->
                lastCommand = command
                Outcome.success(Unit)
            },
            initialState = initialState,
        )
    }

    suspend fun initialize() {
        profileState = MutableStateFlow(initialProfile)

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

    suspend fun verifyGeneralSettingsLoaded(profile: AccountProfile) {
        val expected = initialState.copy(
            name = StringInputField().updateValue(profile.name),
            color = IntegerInputField(value = profile.color),
            avatar = profile.avatar,
        )
        assertThat(turbines.awaitStateItem()).isEqualTo(expected)
    }

    fun pressBack() {
        viewModel.event(GeneralSettingsContract.Event.OnBackPressed)
    }

    suspend fun verifyBackNavigation() {
        assertThat(turbines.awaitEffectItem()).isEqualTo(
            Effect.NavigateBack,
        )
    }

    fun changeName(value: String) {
        viewModel.event(GeneralSettingsContract.Event.OnNameChange(value))
    }

    fun changeColor(value: Int) {
        viewModel.event(GeneralSettingsContract.Event.OnColorChange(value))
    }

    fun changeAvatar(value: Avatar) {
        viewModel.event(GeneralSettingsContract.Event.OnAvatarChange(value))
    }

    suspend fun verifyLastCommand(expected: UpdateGeneralSettingCommand) {
        mviContext.advanceUntilIdle()
        assertThat(lastCommand).isEqualTo(expected)
    }
}

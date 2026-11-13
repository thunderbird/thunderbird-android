package net.thunderbird.feature.account.settings.impl.ui.general

import android.content.Context
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.pressBack
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.ui.setting.emptySettings
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.api.AccountSettingsExternalContract
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

internal class GeneralSettingsScreenKtTest : ComposeTest() {

    private var launchedAccountId: AccountId? = null
    private val launcher = object : AccountSettingsExternalContract.Launcher {
        override fun launchFolderSettings(context: Context, accountId: AccountId) {
            launchedAccountId = accountId
        }

        override fun launchNotificationStyleSettings(context: Context, accountId: AccountId) {
            error("Unexpected notification style launch")
        }

        override fun launchFolderSyncAndPushSettings(context: Context, accountId: AccountId) {
            error("Unexpected folder sync and push launch")
        }
    }

    @Test
    fun `should call onBack when back button is pressed`() {
        val initialState = State()
        val accountId = AccountIdFactory.create()
        val viewModel = FakeGeneralSettingsViewModel(initialState)
        var onBackCounter = 0

        setContentWithTheme {
            GeneralSettingsScreen(
                accountId = accountId,
                onBack = { onBackCounter++ },
                viewModel = viewModel,
                provider = FakeSettingViewProvider(),
                builder = { _, _, _ -> emptySettings() },
                launcher = launcher,
            )
        }

        assertThat(onBackCounter).isEqualTo(0)

        pressBack()

        assertThat(onBackCounter).isEqualTo(1)
    }

    @Test
    fun `should call onBack when navigate back effect received`() {
        val initialState = State()
        val accountId = AccountIdFactory.create()
        val viewModel = FakeGeneralSettingsViewModel(initialState)
        var onBackCounter = 0

        setContentWithTheme {
            GeneralSettingsScreen(
                accountId = accountId,
                onBack = { onBackCounter++ },
                viewModel = viewModel,
                provider = FakeSettingViewProvider(),
                builder = { _, _, _ -> emptySettings() },
                launcher = launcher,
            )
        }

        assertThat(onBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateBack)

        assertThat(onBackCounter).isEqualTo(1)
    }

    @Test
    fun `should delegate folder settings to the legacy launcher`() {
        // Arrange
        val accountId = AccountIdFactory.create()
        val viewModel = FakeGeneralSettingsViewModel(State())
        setContentWithTheme {
            GeneralSettingsScreen(
                accountId = accountId,
                onBack = {},
                viewModel = viewModel,
                provider = FakeSettingViewProvider(),
                builder = { _, _, _ -> emptySettings() },
                launcher = launcher,
            )
        }

        // Act
        viewModel.effect(Effect.OpenManageFolders)
        composeTestRule.waitForIdle()

        // Assert
        assertThat(launchedAccountId).isEqualTo(accountId)
    }
}

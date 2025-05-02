package net.thunderbird.feature.account.settings.impl.ui.general

import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.pressBack
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

internal class GeneralSettingsScreenKtTest : ComposeTest() {

    @Test
    fun `should call onBack when back button is pressed`() {
        val initialState = State()
        val accountId = AccountId.create()
        val viewModel = FakeGeneralSettingsViewModel(initialState)
        var onBackCounter = 0

        setContentWithTheme {
            GeneralSettingsScreen(
                accountId = accountId,
                onBack = { onBackCounter++ },
                viewModel = viewModel,
            )
        }

        assertThat(onBackCounter).isEqualTo(0)

        pressBack()

        assertThat(onBackCounter).isEqualTo(1)
    }

    @Test
    fun `should call onBack when navigate back effect received`() {
        val initialState = State()
        val accountId = AccountId.create()
        val viewModel = FakeGeneralSettingsViewModel(initialState)
        var onBackCounter = 0

        setContentWithTheme {
            GeneralSettingsScreen(
                accountId = accountId,
                onBack = { onBackCounter++ },
                viewModel = viewModel,
            )
        }

        assertThat(onBackCounter).isEqualTo(0)

        viewModel.effect(Effect.NavigateBack)

        assertThat(onBackCounter).isEqualTo(1)
    }
}

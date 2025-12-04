package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.SettingsBuilder
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
internal fun GeneralSettingsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    viewModel: GeneralSettingsContract.ViewModel = koinViewModel<GeneralSettingsViewModel> {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: SettingsBuilder = koinInject(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateBack -> onBack()
        }
    }

    BackHandler(onBack = onBack)

    GeneralSettingsContent(
        state = state.value,
        onEvent = { dispatch(it) },
        provider = provider,
        builder = builder,
    )
}

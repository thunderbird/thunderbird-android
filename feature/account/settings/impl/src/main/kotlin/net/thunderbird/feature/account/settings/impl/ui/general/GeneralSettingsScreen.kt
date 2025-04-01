package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.mvi.observe
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun GeneralSettingsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    viewModel: GeneralSettingsContract.ViewModel = koinViewModel<GeneralSettingsViewModel> {
        parametersOf(accountId)
    },
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
    )
}

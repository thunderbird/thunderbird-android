package net.thunderbird.feature.account.settings.impl.ui.fetchingMail.advanced

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.NoOpUpdate
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("ContextCastToActivity")
@Composable
internal fun AdvancedFetchingMailSettingsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    viewModel: FetchingMailSettingsContract.ViewModel = koinViewModel<FetchingMailSettingsViewModel> {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: FetchingMailSettingsContract.SettingsBuilder = koinInject(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateBack -> {
                onBack()
            }

            else -> {
                NoOpUpdate
            }
        }
    }
    BackHandler(onBack = onBack)

    AdvancedFetchingMailSettingsContent(
        state = state.value,
        onEvent = { dispatch(it) },
        provider = provider,
        builder = builder,
    )
}

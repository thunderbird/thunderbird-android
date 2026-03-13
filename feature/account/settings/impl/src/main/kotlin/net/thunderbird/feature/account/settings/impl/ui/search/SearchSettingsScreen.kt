package net.thunderbird.feature.account.settings.impl.ui.search

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
internal fun SearchSettingsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    viewModel: SearchSettingsContract.ViewModel = koinViewModel<SearchSettingsViewModel> {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: SearchSettingsContract.SettingsBuilder = koinInject(),
    appNameProvider: AppNameProvider = koinInject(),

) {
    val (state, dispatch) = viewModel.observe {
        onBack()
    }
    val activity = LocalActivity.current as ComponentActivity
    BackHandler(onBack = onBack)

    SearchSettingContent(
        state = state.value,
        onEvent = { dispatch(it) },
        provider = provider,
        builder = builder,
        appNameProvider = appNameProvider,
        onAccountRemove = {
            activity.setResult(Activity.RESULT_OK)
            activity.finish()
        },
    )
}

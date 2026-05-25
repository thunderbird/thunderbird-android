package net.thunderbird.feature.account.settings.impl.ui.fetchingMail.advanced

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.NoOpUpdate
import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalUuidApi::class)
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
    appNameProvider: AppNameProvider = koinInject(),
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
    val activity = LocalActivity.current as ComponentActivity
    BackHandler(onBack = onBack)

    AdvancedFetchingMailSettingsContent(
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

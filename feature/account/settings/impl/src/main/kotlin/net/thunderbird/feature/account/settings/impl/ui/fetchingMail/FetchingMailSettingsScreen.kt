package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.Effect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalUuidApi::class)
@SuppressLint("ContextCastToActivity")
@Composable
internal fun FetchingMailSettingsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    viewModel: FetchingMailSettingsContract.ViewModel = koinViewModel<FetchingMailSettingsViewModel> {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: FetchingMailSettingsContract.SettingsBuilder = koinInject(),
    appNameProvider: AppNameProvider = koinInject(),
) {
    val context = LocalContext.current
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateBack -> {
                onBack()
            }
            is Effect.NavigateToIncomingServerSettings -> {
                FeatureLauncherActivity.launch(
                    context = context,
                    target = FeatureLauncherTarget.AccountEditIncomingSettings(accountUuid = "${accountId.value}"),
                )
            }
            is Effect.NavigateToAdvancedFetchingMailSettings -> {
                FeatureLauncherActivity.launch(
                    context = context,
                    target = FeatureLauncherTarget.AccountAdvancedFetchingMailSettings(
                        accountUuid = "${accountId.value}",
                    ),
                )
            }
        }
    }
    val activity = LocalActivity.current as ComponentActivity
    BackHandler(onBack = onBack)

    FetchingMailSettingsContent(
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

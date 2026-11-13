package net.thunderbird.feature.account.settings.impl.ui.sendingMail

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.api.AccountSettingsRoute
import net.thunderbird.feature.account.settings.impl.ui.sendingMail.SendingMailSettingContract.Effect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalUuidApi::class)
@SuppressLint("ContextCastToActivity")
@Composable
internal fun SendingMailSettingsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    onFinish: (AccountSettingsRoute) -> Unit,
    viewModel: SendingMailSettingContract.ViewModel = koinViewModel<SendingMailSettingsViewModel> {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: SendingMailSettingContract.SettingsBuilder = koinInject(),
    appNameProvider: AppNameProvider = koinInject(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateBack -> {
                onBack()
            }

            is Effect.NavigateToCompositionDefaults -> {
                onFinish(AccountSettingsRoute.CompositionSettings(accountId.value.toString()))
            }

            is Effect.NavigateToManageIdentities -> {
                onFinish(AccountSettingsRoute.ManageIdentities(accountId.value.toString()))
            }

            is Effect.NavigateToOutGoingServerSettings -> {
                onFinish(AccountSettingsRoute.OutgoingServerSettings(accountId.value.toString()))
            }
        }
    }
    val activity = LocalActivity.current as ComponentActivity
    BackHandler(onBack = onBack)

    SendingMailSettingsContent(
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

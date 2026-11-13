package net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.api.AccountSettingsExternalContract
import net.thunderbird.feature.account.settings.api.AccountSettingsRoute
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.Effect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun SyncAndNotificationsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    onFinish: (AccountSettingsRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncAndNotificationSettingsViewModel = koinViewModel {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: SyncAndNotificationSettingsContract.SettingsBuilder = koinInject(),
    launcher: AccountSettingsExternalContract.Launcher = koinInject(),
) {
    val context = LocalContext.current

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateBack -> {
                onBack()
            }

            is Effect.NavigateToNotificationStyle -> {
                launcher.launchNotificationStyleSettings(context = context, accountId = accountId)
            }

            is Effect.NavigateToFolderSyncAndPush -> {
                launcher.launchFolderSyncAndPushSettings(context = context, accountId = accountId)
            }

            is Effect.NavigateToFetchingAndDataLimits -> {
                onFinish(AccountSettingsRoute.FetchingMailSettings(accountId.value.toString()))
            }
        }
    }
    BackHandler(onBack = onBack)

    SyncAndNotificationSettingsContent(
        state = state.value,
        onEvent = { dispatch(it) },
        provider = provider,
        builder = builder,
        modifier = modifier,
    )
}

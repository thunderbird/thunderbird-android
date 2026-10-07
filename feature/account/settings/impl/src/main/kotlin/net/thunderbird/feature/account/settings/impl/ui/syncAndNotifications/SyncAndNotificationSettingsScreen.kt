package net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun SyncAndNotificationsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncAndNotificationSettingsViewModel = koinViewModel<SyncAndNotificationSettingsViewModel> {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: SyncAndNotificationSettingsContract.SettingsBuilder = koinInject(),
) {
    val (state, dispatch) = viewModel.observe {
        onBack()
    }
    BackHandler(onBack = onBack)

    SyncAndNotificationSettingsContent(modifier)
}

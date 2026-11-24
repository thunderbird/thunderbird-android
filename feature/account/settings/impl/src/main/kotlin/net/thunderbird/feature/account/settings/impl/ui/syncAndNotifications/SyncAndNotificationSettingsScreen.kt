package net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import com.fsck.k9.ui.managefolders.ManageFoldersActivity
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.fsck.k9.ui.settings.account.AccountSettingsFragment
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.Effect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun SyncAndNotificationsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncAndNotificationSettingsViewModel = koinViewModel {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: SyncAndNotificationSettingsContract.SettingsBuilder = koinInject(),
) {
    val context = LocalContext.current
    val activityContext = LocalActivity.current

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateBack -> {
                onBack()
            }

            is Effect.NavigateToNotificationStyle -> {
                AccountSettingsActivity.start(
                    context = context,
                    accountUuid = accountId.value.toString(),
                    startScreenKey = AccountSettingsFragment.PREFERENCE_NOTIFICATION_STYLE,
                )
            }

            is Effect.NavigateToFolderSyncAndPush -> {
                ManageFoldersActivity.launch(
                    requireNotNull(activityContext) {
                        "Can't initialize ManageFoldersActivity. The activityContext was null."
                    },
                    accountId,
                )
            }

            is Effect.NavigateToFetchingAndDataLimits -> {
                FeatureLauncherActivity.launch(
                    context = context,
                    target = FeatureLauncherTarget.AccountFetchingMailSettings(
                        accountId = accountId,
                    ),
                )
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

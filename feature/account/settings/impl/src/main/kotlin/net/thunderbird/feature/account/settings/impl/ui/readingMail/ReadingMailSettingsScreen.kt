package net.thunderbird.feature.account.settings.impl.ui.readingMail

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.mvi.observe
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.readingMail.ReadingMailSettingsContract.SettingsBuilder
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@SuppressLint("ContextCastToActivity")
@Composable
internal fun ReadingMailSettingsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    viewModel: ReadingMailSettingsContract.ViewModel = koinViewModel<ReadingMailSettingsViewModel> {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: SettingsBuilder = koinInject(),
    appNameProvider: AppNameProvider = koinInject(),
) {
    val (state, dispatch) = viewModel.observe {
        onBack()
    }
    val activity = LocalActivity.current as ComponentActivity
    BackHandler(onBack = onBack)

    ReadingMailSettingsContent(
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

package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.mvi.observe
import com.eygraber.uri.toKmpUri
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.SettingsBuilder
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
internal fun GeneralSettingsScreen(
    accountId: AccountId,
    onBack: () -> Unit,
    viewModel: GeneralSettingsContract.ViewModel = koinViewModel<GeneralSettingsViewModel> {
        parametersOf(accountId)
    },
    provider: SettingViewProvider = koinInject(),
    builder: SettingsBuilder = koinInject(),
) {
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.event(Event.OnAvatarImagePicked(uri.toKmpUri()))
        }
    }

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateBack -> onBack()
            is Effect.OpenAvatarImagePicker -> {
                imagePicker.launch("image/jpeg")
            }
        }
    }

    BackHandler(onBack = onBack)

    GeneralSettingsContent(
        state = state.value,
        onEvent = { dispatch(it) },
        provider = provider,
        builder = builder,
    )
}

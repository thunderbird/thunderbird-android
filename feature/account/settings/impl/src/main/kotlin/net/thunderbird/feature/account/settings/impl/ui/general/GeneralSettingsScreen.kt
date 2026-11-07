package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.eygraber.uri.toKmpUri
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.fsck.k9.ui.settings.account.AccountSettingsFragment
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.SettingsBuilder
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
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
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.event(Event.OnAvatarImagePicked(uri.toKmpUri()))
        }
    }

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateBack -> onBack()

            is Effect.OpenAvatarImagePicker -> {
                imagePicker.launch(arrayOf("image/jpeg", "image/png"))
            }

            is Effect.OpenManageFolders -> {
                AccountSettingsActivity.start(
                    context = context,
                    accountUuid = accountId.value.toString(),
                    startScreenKey = AccountSettingsFragment.PREFERENCE_FOLDERS,
                )
            }
        }
    }

    BackHandler(onBack = onBack)

    GeneralSettingsContent(
        accountId = accountId,
        state = state.value,
        onEvent = { dispatch(it) },
        provider = provider,
        builder = builder,
    )
}

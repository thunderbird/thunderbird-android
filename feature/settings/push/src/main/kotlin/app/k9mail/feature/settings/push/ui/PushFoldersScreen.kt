package app.k9mail.feature.settings.push.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.Icons
import app.k9mail.feature.settings.push.R
import app.k9mail.feature.settings.push.ui.PushFoldersContract.Effect
import app.k9mail.feature.settings.push.ui.PushFoldersContract.Event
import app.k9mail.feature.settings.push.ui.PushFoldersContract.ViewModel
import com.fsck.k9.Account.FolderMode
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun PushFoldersScreen(
    accountUuid: String,
    onOptionSelected: (FolderMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<PushFoldersViewModel> { parametersOf(accountUuid) },
) {
    val contactsPermissionLauncher = rememberLauncherForActivityResult(RequestAlarmPermission()) {
        viewModel.event(Event.AlarmPermissionResult)
    }

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            is Effect.OptionSelected -> onOptionSelected(effect.option)
            Effect.RequestAlarmPermission -> contactsPermissionLauncher.launch(Unit)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_push_title),
                navigationIcon = {
                    ButtonIcon(
                        onClick = onBack,
                        imageVector = Icons.Outlined.arrowBack,
                    )
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        PushFoldersContent(
            state = state.value,
            onEvent = dispatch,
            innerPadding = innerPadding,
        )
    }
}

package net.thunderbird.feature.mail.message.list.internal.ui.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.organism.BasicDialog
import app.k9mail.core.ui.compose.designsystem.organism.BasicDialogDefaults
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.message.list.internal.R
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.Event
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.State
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.ViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun SetupArchiveFolderDialog(
    accountId: AccountId,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<ViewModel> { parametersOf(accountId) },
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            SetupArchiveFolderDialogContract.Effect.DismissDialog -> onDismissDialog()
        }
    }

    LaunchedEffect(onDismissDialog, state.value) {
        if (state.value is State.Closed) {
            onDismissDialog()
        }
    }

    SetupArchiveFolderDialog(
        state = state.value,
        onNextClick = { dispatch(Event.MoveNext) },
        onDoneClick = { dispatch(Event.OnDoneClicked) },
        onDismissRequest = { dispatch(Event.OnDismissClicked) },
        onDismissClick = { dispatch(Event.OnDismissClicked) },
        onDoNotShowAgainChange = { isChecked ->
            dispatch(
                Event.OnDoNotShowDialogAgainChanged(
                    isChecked = isChecked,
                ),
            )
        },
        onFolderSelect = { folder -> dispatch(Event.OnFolderSelected(folder)) },
        onCreateAndSetClick = { folderName -> dispatch(Event.OnCreateFolderClicked(folderName)) },
        modifier = modifier,
    )
}

@Composable
internal fun SetupArchiveFolderDialog(
    state: State,
    modifier: Modifier = Modifier,
    onNextClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onDismissClick: () -> Unit = {},
    onDoNotShowAgainChange: (Boolean) -> Unit = {},
    onFolderSelect: (RemoteFolder) -> Unit = {},
    onCreateAndSetClick: (folderName: String) -> Unit = {},
) {
    if (state !is State.Closed) {
        val canBeDismissed = remember(state) {
            state !is State.CreateArchiveFolder || state.syncingMessage.isNullOrBlank()
        }
        var folderName by rememberSaveable(state) {
            mutableStateOf(
                (state as? State.CreateArchiveFolder)?.folderName ?: "",
            )
        }
        BasicDialog(
            headlineText = when (state) {
                is State.ChooseArchiveFolder -> stringResource(R.string.setup_archive_folder_dialog_set_archive_folder)
                is State.CreateArchiveFolder -> stringResource(R.string.setup_archive_folder_dialog_create_new_folder)
                is State.EmailCantBeArchived ->
                    stringResource(R.string.setup_archive_folder_dialog_email_can_not_be_archived)

                else -> error("Invalid state: $state")
            },
            supportingText = when (state) {
                is State.EmailCantBeArchived ->
                    stringResource(R.string.setup_archive_folder_dialog_configure_archive_folder)

                else -> null
            },
            onDismissRequest = onDismissRequest,
            content = {
                SetupArchiveFolderDialogContent(
                    state = state,
                    folderName = folderName,
                    onFolderSelect = onFolderSelect,
                    onFolderNameChange = { newFolderName -> folderName = newFolderName },
                )
            },
            buttons = {
                SetupArchiveFolderDialogButtons(
                    state = state,
                    folderName = folderName,
                    onDoneClick = onDoneClick,
                    onNextClick = onNextClick,
                    onDismissClick = onDismissClick,
                    onCreateAndSetClick = onCreateAndSetClick,
                    onDoNotShowAgainChange = onDoNotShowAgainChange,
                )
            },
            properties = DialogProperties(
                dismissOnBackPress = canBeDismissed,
                dismissOnClickOutside = canBeDismissed,
            ),
            contentPadding = if (state is State.EmailCantBeArchived) {
                PaddingValues()
            } else {
                BasicDialogDefaults.contentPadding
            },
            showDividers = state !is State.EmailCantBeArchived,
            modifier = modifier,
        )
    }
}

@Composable
private fun SetupArchiveFolderDialogContent(
    state: State,
    folderName: String,
    onFolderSelect: (RemoteFolder) -> Unit,
    onFolderNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith slideOutHorizontally(
                targetOffsetX = { -it },
            )
        },
        contentKey = { it::class },
        modifier = modifier,
    ) { state ->
        when (state) {
            is State.ChooseArchiveFolder -> ChooseArchiveFolderDialogContent(
                state = state,
                onFolderSelect = onFolderSelect,
            )

            is State.CreateArchiveFolder -> CreateNewArchiveFolderDialogContent(
                folderName = folderName,
                syncingMessage = state.syncingMessage,
                errorMessage = state.errorMessage,
                onFolderNameChange = onFolderNameChange,
            )

            else -> Spacer(modifier = Modifier.height(MainTheme.spacings.half))
        }
    }
}

@Composable
private fun RowScope.SetupArchiveFolderDialogButtons(
    state: State,
    folderName: String,
    onDoneClick: () -> Unit,
    onNextClick: () -> Unit,
    onDismissClick: () -> Unit,
    onCreateAndSetClick: (String) -> Unit,
    onDoNotShowAgainChange: (Boolean) -> Unit,
) {
    when (state) {
        is State.ChooseArchiveFolder -> ChooseArchiveFolderDialogButtons(
            state = state,
            onDoneClick = onDoneClick,
            onCreateNewFolderClick = onNextClick,
        )

        is State.Closed -> Unit

        is State.CreateArchiveFolder -> CreateNewArchiveFolderDialogButtons(
            isSynchronizing = state.syncingMessage?.isNotBlank() == true,
            onCancelClick = onDismissClick,
            onCreateAndSetClick = { onCreateAndSetClick(folderName) },
        )

        is State.EmailCantBeArchived -> EmailCantBeArchivedDialogButtons(
            state = state,
            onSetArchiveFolderClick = onNextClick,
            onSkipClick = onDismissClick,
            onDoNotShowAgainChange = onDoNotShowAgainChange,
        )
    }
}

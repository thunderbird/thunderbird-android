package net.thunderbird.feature.mail.message.list.ui.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.window.DialogProperties
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.organism.BasicDialog
import app.k9mail.core.ui.compose.designsystem.organism.BasicDialogDefaults
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.message.list.R
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract.Event
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun SetupArchiveFolderDialog(
    accountUuid: String,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupArchiveFolderDialogContract.ViewModel = koinViewModel<SetupArchiveFolderDialogViewModel> { parametersOf(accountUuid) },
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            SetupArchiveFolderDialogContract.Effect.DismissDialog -> onDismissDialog()
        }
    }

    SetupArchiveFolderDialog(
        state = state.value,
        onNextClick = { dispatch(Event.MoveNext) },
        onDoneClick = { dispatch(Event.OnDoneClicked) },
        onDismissRequest = onDismissDialog,
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
private fun SetupArchiveFolderDialog(
    state: SetupArchiveFolderDialogContract.State,
    modifier: Modifier = Modifier,
    onNextClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onDismissClick: () -> Unit = {},
    onDoNotShowAgainChange: (Boolean) -> Unit = {},
    onFolderSelect: (RemoteFolder) -> Unit = {},
    onCreateAndSetClick: (folderName: String) -> Unit = {},
) {
    if (state !is SetupArchiveFolderDialogContract.State.Closed) {
        val canBeDismissed = remember(state) {
            state !is SetupArchiveFolderDialogContract.State.CreateArchiveFolder || state.syncingMessage.isNullOrBlank()
        }
        var folderName by rememberSaveable(state) {
            mutableStateOf(
                (state as? SetupArchiveFolderDialogContract.State.CreateArchiveFolder)?.folderName ?: "",
            )
        }
        BasicDialog(
            onDismissRequest = onDismissRequest,
            headlineText = when (state) {
                is SetupArchiveFolderDialogContract.State.ChooseArchiveFolder -> stringResource(R.string.setup_archive_folder_dialog_set_archive_folder)
                is SetupArchiveFolderDialogContract.State.CreateArchiveFolder -> stringResource(R.string.setup_archive_folder_dialog_create_new_folder)
                is SetupArchiveFolderDialogContract.State.EmailCantBeArchived ->
                    stringResource(R.string.setup_archive_folder_dialog_email_can_not_be_archived)

                else -> error("Invalid state: $state")
            },
            supportingText = when (state) {
                is SetupArchiveFolderDialogContract.State.EmailCantBeArchived ->
                    stringResource(R.string.setup_archive_folder_dialog_configure_archive_folder)

                else -> null
            },
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
            contentPadding = if (state is SetupArchiveFolderDialogContract.State.EmailCantBeArchived) {
                PaddingValues()
            } else {
                BasicDialogDefaults.contentPadding
            },
            showDividers = state !is SetupArchiveFolderDialogContract.State.EmailCantBeArchived,
            modifier = modifier,
        )
    }
}

@Composable
private fun SetupArchiveFolderDialogContent(
    state: SetupArchiveFolderDialogContract.State,
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
            is SetupArchiveFolderDialogContract.State.ChooseArchiveFolder -> ChooseArchiveFolderDialogContent(
                state = state,
                onFolderSelect = onFolderSelect,
            )

            is SetupArchiveFolderDialogContract.State.CreateArchiveFolder -> CreateNewArchiveFolderDialogContent(
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
    state: SetupArchiveFolderDialogContract.State,
    folderName: String,
    onDoneClick: () -> Unit,
    onNextClick: () -> Unit,
    onDismissClick: () -> Unit,
    onCreateAndSetClick: (String) -> Unit,
    onDoNotShowAgainChange: (Boolean) -> Unit,
) {
    when (state) {
        is SetupArchiveFolderDialogContract.State.ChooseArchiveFolder -> ChooseArchiveFolderDialogButtons(
            state = state,
            onDoneClick = onDoneClick,
            onCreateNewFolderClick = onNextClick,
        )

        SetupArchiveFolderDialogContract.State.Closed -> Unit
        is SetupArchiveFolderDialogContract.State.CreateArchiveFolder -> CreateNewArchiveFolderDialogButtons(
            isSynchronizing = state.syncingMessage?.isNotBlank() == true,
            onCancelClick = onDismissClick,
            onCreateAndSetClick = { onCreateAndSetClick(folderName) },
        )

        is SetupArchiveFolderDialogContract.State.EmailCantBeArchived -> EmailCantBeArchivedDialogButtons(
            state = state,
            onSetArchiveFolderClick = onNextClick,
            onSkipClick = onDismissClick,
            onDoNotShowAgainChange = onDoNotShowAgainChange,
        )
    }
}

private class SetupArchiveFolderDialogParamCol : CollectionPreviewParameterProvider<SetupArchiveFolderDialogContract.State>(
    setOf(
        SetupArchiveFolderDialogContract.State.EmailCantBeArchived(isDoNotShowDialogAgainChecked = true),
        SetupArchiveFolderDialogContract.State.EmailCantBeArchived(isDoNotShowDialogAgainChecked = false),
        SetupArchiveFolderDialogContract.State.ChooseArchiveFolder(isLoadingFolders = false, folders = emptyList()),
        SetupArchiveFolderDialogContract.State.ChooseArchiveFolder(isLoadingFolders = true, folders = emptyList()),
        SetupArchiveFolderDialogContract.State.ChooseArchiveFolder(
            isLoadingFolders = false,
            folders = List(size = 5) {
                RemoteFolder(
                    id = it.toLong(),
                    serverId = "$it",
                    name = "Folder 1",
                    type = FolderType.REGULAR,
                )
            },
        ),
        SetupArchiveFolderDialogContract.State.CreateArchiveFolder(syncingMessage = null, folderName = ""),
        SetupArchiveFolderDialogContract.State.CreateArchiveFolder(syncingMessage = "any message", folderName = ""),
    ),
)

@PreviewLightDark
@Composable
private fun Preview(
    @PreviewParameter(SetupArchiveFolderDialogParamCol::class) state: SetupArchiveFolderDialogContract.State,
) {
    ThunderbirdTheme2 {
        Surface(modifier = Modifier.fillMaxSize()) {
            SetupArchiveFolderDialog(state = state)
        }
    }
}

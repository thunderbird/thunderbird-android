package app.k9mail.feature.settings.push.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.feature.settings.push.R
import app.k9mail.feature.settings.push.ui.PushFoldersContract.Event
import app.k9mail.feature.settings.push.ui.PushFoldersContract.State
import com.fsck.k9.Account.FolderMode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

private val listItemMinHeight = 56.dp
private val listItemContentStart = 72.dp

@Composable
internal fun PushFoldersContent(
    state: State,
    onEvent: (Event) -> Unit,
    innerPadding: PaddingValues,
) {
    ResponsiveContent(
        modifier = Modifier.padding(innerPadding),
    ) {
        if (state.isLoading) {
            LoadingIndicator()
        } else {
            ContentView(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        LoadingView()
    }
}

@Composable
private fun ContentView(
    state: State,
    onEvent: (Event) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(state = rememberScrollState()),
    ) {
        if (state.showPermissionPrompt) {
            PermissionPrompt(onEvent)
        }

        PushFolderOptionRadioGroup(
            selectedOption = state.selectedOption,
            options = persistentListOf(
                FolderMode.ALL,
                FolderMode.FIRST_CLASS,
                FolderMode.FIRST_AND_SECOND_CLASS,
                FolderMode.NOT_SECOND_CLASS,
                FolderMode.NONE,
            ),
            onSelected = { onEvent(Event.OptionSelected(it)) },
            enabled = !state.showPermissionPrompt,
            modifier = Modifier.padding(vertical = MainTheme.spacings.double),
        )
    }
}

@Composable
private fun PermissionPrompt(onEvent: (Event) -> Unit) {
    Column(modifier = Modifier.padding(all = MainTheme.spacings.double)) {
        TextBody1(text = stringResource(R.string.settings_push_permission_info_text))

        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        Button(
            text = stringResource(R.string.settings_push_grant_permission_button),
            onClick = { onEvent(Event.GrantAlarmPermissionClicked) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun PushFolderOptionRadioGroup(
    selectedOption: FolderMode,
    options: PersistentList<FolderMode>,
    onSelected: (FolderMode) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    Column(modifier = modifier) {
        for (option in options) {
            val isSelected = option == selectedOption
            val isEnabled = enabled || option == FolderMode.NONE

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = listItemMinHeight)
                    .selectable(
                        selected = isSelected,
                        enabled = isEnabled,
                        onClick = { onSelected(option) },
                    ),
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelected(option) },
                    enabled = isEnabled,
                    modifier = Modifier.width(listItemContentStart),
                )
                TextBody1(
                    text = option.toResourceString(resources),
                    modifier = Modifier
                        .padding(end = MainTheme.spacings.double)
                        .disabledText(!isEnabled, ContentAlpha.disabled),
                )
            }
        }
    }
}

@Stable
private fun Modifier.disabledText(disabled: Boolean, disabledAlpha: Float): Modifier {
    return if (disabled) {
        alpha(disabledAlpha)
    } else {
        this
    }
}

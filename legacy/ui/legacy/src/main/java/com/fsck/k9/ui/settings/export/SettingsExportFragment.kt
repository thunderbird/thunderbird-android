package com.fsck.k9.ui.settings.export

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import app.k9mail.core.android.common.activity.CreateDocumentResultContract
import com.fsck.k9.ui.R
import net.thunderbird.components.ui.bolt.atom.CircularProgressIndicator
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.Switch
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsExportFragment : Fragment() {
    private val viewModel: SettingsExportViewModel by viewModel()
    private val themeProvider: FeatureThemeProvider by inject()


    private var onDocumentPicked: ((Uri) -> Unit)? = null
    private var onDocumentPickCanceled: (() -> Unit)? = null

    private val createDocumentLauncher: ActivityResultLauncher<CreateDocumentResultContract.Input> =
        registerForActivityResult(
            CreateDocumentResultContract(),
        ) { contentUri ->
            if (contentUri != null) {
                onDocumentPicked?.invoke(contentUri)
            } else {
                onDocumentPickCanceled?.invoke()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val(state,dispatch) = viewModel.observe { effect ->
                   when(effect) {
                       is SettingsExportContract.Effect.PickDocument -> {
                           pickDocument(effect.fileNameSuggestion, effect.mimeType)
                       }
                       is SettingsExportContract.Effect.ShareDocument -> {
                           shareDocument(effect.contentUri, effect.mimeType)
                       }
                   }
                }

                onDocumentPicked = { contentUri ->
                    dispatch(SettingsExportContract.Event.OnDocumentPicked(contentUri))
                }
                onDocumentPickCanceled = {
                    dispatch(SettingsExportContract.Event.OnDocumentPickCanceled)
                }

                themeProvider.WithTheme {
                    SettingsExportScreen(
                        onExportClick = {
                            dispatch(SettingsExportContract.Event.OnExportClick)
                        },
                        onShareClick = {
                            dispatch(SettingsExportContract.Event.OnShareClick)
                        },
                        state = state.value,
                        onSettingsListItemSelect = { itemId, isSelected ->
                            dispatch(SettingsExportContract.Event.OnSettingsItemSelectionChange(itemId,isSelected))
                        }

                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if(savedInstanceState !=null) {
            viewModel.initializeFromSavedState(savedInstanceState)
        }
    }




    private fun pickDocument(fileNameSuggestion: String, mimeType: String) {
        createDocumentLauncher.launch(
            input = CreateDocumentResultContract.Input(
                title = fileNameSuggestion,
                mimeType = mimeType,
            ),
        )
    }

    private fun shareDocument(contentUri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, null))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveInstanceState(outState)
    }
}


@Composable
fun SettingsExportScreen(
    state: SettingsExportContract.State,
    onExportClick: () -> Unit,
    onShareClick: () -> Unit,
    onSettingsListItemSelect: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(
                    items = state.settingsList,
                    key = { item ->
                        item.stableId
                    },
                ) { setting ->
                    SettingsExportItem(
                        setting = setting,
                        checked = setting.selected,
                        onCheckedChange = { isChecked ->
                            onSettingsListItemSelect(setting.stableId, isChecked)
                        },
                    )
                }
            }

            SettingsExportBottomBar(
                state = state,
                onExportClick = onExportClick,
                onShareClick = onShareClick,
            )
        }
    }
}

@Composable
fun SettingsExportItem(
    setting: SettingsListItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(BoltTheme.spacings.double),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            TextTitleMedium(
                text = when (setting) {
                    is SettingsListItem.GeneralSettings ->
                        "General Settings"

                    is SettingsListItem.Account ->
                        setting.displayName
                },
            )

            if (setting is SettingsListItem.Account) {
                TextBodyMedium(text = setting.email)
            }
        }

        Spacer(
            modifier = Modifier.width(BoltTheme.spacings.default),
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}


@Composable
private fun SettingsExportBottomBar(
    state: SettingsExportContract.State,
    onExportClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DividerHorizontal()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BoltTheme.sizes.large)
                .padding(
                    horizontal = BoltTheme.spacings.double,
                    vertical = BoltTheme.spacings.default,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            val statusLabel = when (state.statusText) {
                StatusText.EXPORT_SUCCESS ->
                    stringResource(R.string.settings_export_success_generic)

                StatusText.PROGRESS ->
                    stringResource(R.string.settings_export_progress_text)

                StatusText.EXPORT_FAILURE ->
                    stringResource(R.string.settings_export_failure)

                StatusText.HIDDEN -> ""
            }

            if (state.statusText != StatusText.HIDDEN) {
                TextLabelLarge(text = statusLabel)
            }

            Spacer(Modifier.weight(1f))

            when {
                state.isProgressVisible -> CircularProgressIndicator()

                state.isShareButtonVisible -> ButtonFilled(
                    text = stringResource(R.string.settings_export_share_button),
                    onClick = onShareClick,
                )

                state.exportButtonState != ButtonState.GONE &&
                    state.exportButtonState != ButtonState.INVISIBLE -> ButtonFilled(
                    text = stringResource(R.string.settings_export_button),
                    onClick = onExportClick,
                    enabled = state.exportButtonState == ButtonState.ENABLED && state.settingsList.any { it.selected },
                )
            }
        }
    }
}


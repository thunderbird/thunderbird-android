package net.thunderbird.feature.debug.settings.featureflag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableMap
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.organism.AlertDialog
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.ui.component.molecule.FeatureFlagItem
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.debug.settings.R
import net.thunderbird.feature.debug.settings.navigation.SecretDebugSettingsRoute
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DebugFeatureFlagSection(
    showUnsavedChangesDialog: Boolean,
    onNavigateBack: () -> Unit,
    onFinish: (SecretDebugSettingsRoute.Tab) -> Unit,
    onFeatureFlagChange: (pendingOverrides: ImmutableMap<FeatureFlagKey, Boolean>) -> Unit,
    onStayClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebugFeatureFlagSectionViewModel = koinViewModel<DebugFeatureFlagSectionViewModel>(),
) {
    val (state, dispatchEvent) = viewModel.observe { effect ->
        when (effect) {
            is DebugFeatureFlagSectionContract.Effect.NotifyPendingChanges ->
                onFeatureFlagChange(effect.pendingOverrides)

            is DebugFeatureFlagSectionContract.Effect.RestartMainActivity ->
                onFinish(SecretDebugSettingsRoute.Tab.FeatureFlag)
        }
    }
    DebugFeatureFlagSection(
        state = state.value,
        showUnsavedChangesDialog = showUnsavedChangesDialog,
        onNavigateBack = onNavigateBack,
        onToggleFlagChange = { dispatchEvent(DebugFeatureFlagSectionContract.Event.OnToggle(key = it)) },
        onApplyChangesClick = { dispatchEvent(DebugFeatureFlagSectionContract.Event.ApplyChanges) },
        onRestoreDefaultClick = { dispatchEvent(DebugFeatureFlagSectionContract.Event.RestoreDefaults) },
        onStayClick = onStayClick,
        modifier = modifier,
    )
}

@Composable
internal fun DebugFeatureFlagSection(
    state: DebugFeatureFlagSectionContract.State,
    showUnsavedChangesDialog: Boolean,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onToggleFlagChange: (FeatureFlagKey) -> Unit = {},
    onApplyChangesClick: () -> Unit = {},
    onRestoreDefaultClick: () -> Unit = {},
    onStayClick: () -> Unit = {},
) {
    var isShowingDialog by remember { mutableStateOf(false) }
    LaunchedEffect(showUnsavedChangesDialog) {
        if (showUnsavedChangesDialog && !isShowingDialog) {
            isShowingDialog = true
        }
    }
    BackHandler {
        if (state.pendingOverrides.isNotEmpty()) {
            isShowingDialog = true
        } else {
            onNavigateBack()
        }
    }

    if (isShowingDialog) {
        UnsavedChangesDialog(
            onStayClick = {
                isShowingDialog = false
                onStayClick()
            },
            onNavigateBack = onNavigateBack,
            onDismissRequest = { isShowingDialog = false },
        )
    }

    Column(
        modifier = modifier,
    ) {
        ButtonRow(
            state = state,
            onRestoreDefaultClick = onRestoreDefaultClick,
            onApplyChangesClick = onApplyChangesClick,
        )

        val flags = remember(state.defaults) { state.defaults.toList() }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
            contentPadding = PaddingValues(
                start = BoltTheme.spacings.default,
                end = BoltTheme.spacings.default,
                bottom = BoltTheme.spacings.triple,
            ),
        ) {
            itemsIndexed(items = flags) { index, (key, flagEnabled) ->
                val isOverridden = remember(state.overrides, state.pendingOverrides) {
                    val override = state.pendingOverrides[key] ?: state.overrides[key]
                    override != null && override != flagEnabled
                }
                FeatureFlagItem(
                    key = key,
                    overrides = state.overrides,
                    pendingOverrides = state.pendingOverrides,
                    flagEnabled = flagEnabled,
                    isOverridden = isOverridden,
                    onToggleFlagChange = onToggleFlagChange,
                )
            }
        }
    }
}

@Composable
private fun UnsavedChangesDialog(
    onStayClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = stringResource(R.string.debug_settings_feature_flag_unsaved_changes),
        text = stringResource(R.string.debug_settings_feature_flag_unsaved_changes_content_text),
        confirmText = stringResource(R.string.debug_settings_feature_flag_unsaved_changes_stay_button),
        onConfirmClick = onStayClick,
        dismissText = stringResource(R.string.debug_settings_feature_flag_unsaved_changes_leave_button),
        onDismissRequest = onDismissRequest,
        onDismissClick = onNavigateBack,
    )
}

@Composable
private fun ButtonRow(
    state: DebugFeatureFlagSectionContract.State,
    onRestoreDefaultClick: () -> Unit,
    onApplyChangesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ButtonText(
            text = stringResource(R.string.debug_settings_feature_flag_restore_default_values),
            onClick = onRestoreDefaultClick,
            enabled = state.overrides.isNotEmpty() || state.pendingOverrides.isNotEmpty(),
        )
        ButtonFilled(
            text = stringResource(R.string.debug_settings_feature_flag_apply_changes),
            onClick = onApplyChangesClick,
            enabled = state.pendingOverrides.isNotEmpty(),
        )
    }
}

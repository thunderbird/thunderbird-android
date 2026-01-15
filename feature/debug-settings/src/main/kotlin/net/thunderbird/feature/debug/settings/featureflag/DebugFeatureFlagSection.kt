package net.thunderbird.feature.debug.settings.featureflag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Switch
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.organism.AlertDialog
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableMap
import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.feature.debug.settings.R
import net.thunderbird.feature.debug.settings.navigation.SecretDebugSettingsRoute
import org.koin.androidx.compose.koinViewModel

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
        onToggleFlagChange = { dispatchEvent(DebugFeatureFlagSectionContract.Event.OnToggle(flag = it)) },
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
    onToggleFlagChange: (FeatureFlag) -> Unit = {},
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
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            contentPadding = PaddingValues(
                start = MainTheme.spacings.default,
                end = MainTheme.spacings.default,
                bottom = MainTheme.spacings.triple,
            ),
        ) {
            itemsIndexed(items = flags) { index, (key, flag) ->
                val isOverridden = remember(state.overrides, state.pendingOverrides) {
                    val override = state.pendingOverrides[key] ?: state.overrides[key]
                    override != null && override != flag.enabled
                }
                FeatureFlagItem(
                    state = state,
                    key = key,
                    flag = flag,
                    isOverridden = isOverridden,
                    showDivider = index > 0,
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

@Composable
private fun FeatureFlagItem(
    state: DebugFeatureFlagSectionContract.State,
    key: FeatureFlagKey,
    flag: FeatureFlag,
    isOverridden: Boolean,
    showDivider: Boolean,
    onToggleFlagChange: (FeatureFlag) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showDivider) {
            DividerHorizontal(modifier = Modifier.padding(bottom = MainTheme.spacings.default))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch, onClick = { onToggleFlagChange(flag) })
                .padding(start = MainTheme.spacings.default),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
            ) {
                val modifiedIndicator = stringResource(R.string.debug_settings_feature_flag_modified_indicator)
                TextBodyLarge(
                    text = buildAnnotatedString {
                        if (state.pendingOverrides.containsKey(key)) {
                            withStyle(SpanStyle(color = MainTheme.colors.error)) {
                                append(modifiedIndicator)
                            }
                        }
                        append(key.key)
                    },
                )
                if (isOverridden) {
                    TextLabelSmall(
                        text = buildAnnotatedString {
                            append(
                                stringResource(R.string.debug_settings_feature_flag_overridden),
                            )
                            withStyle(SpanStyle(color = MainTheme.colors.info)) {
                                append(
                                    stringResource(
                                        R.string.debug_settings_feature_flag_default_value,
                                        flag.enabled,
                                    ),
                                )
                            }
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.width(MainTheme.spacings.double))
            Switch(
                checked = state.pendingOverrides[key] ?: state.overrides[key] ?: flag.enabled,
                onCheckedChange = { onToggleFlagChange(flag) },
                modifier = Modifier.padding(end = MainTheme.spacings.default),
            )
        }
    }
}

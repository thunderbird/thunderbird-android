package net.thunderbird.core.featureflag.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.Switch
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.organism.snackbar.SnackbarHost
import net.thunderbird.components.ui.bolt.organism.snackbar.SnackbarHostState
import net.thunderbird.components.ui.bolt.organism.snackbar.rememberSnackbarHostState
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.core.featureflag.resources.Res
import net.thunderbird.core.featureflag.resources.remote_feature_flag_screen_catalog_location
import net.thunderbird.core.featureflag.resources.remote_feature_flag_screen_enabled_description
import net.thunderbird.core.featureflag.resources.remote_feature_flag_screen_enabled_flags
import net.thunderbird.core.featureflag.resources.remote_feature_flag_screen_enabled_title
import net.thunderbird.core.featureflag.resources.remote_feature_flag_screen_no_flags_found
import net.thunderbird.core.featureflag.resources.remote_feature_flag_screen_top_bar_title
import net.thunderbird.core.featureflag.ui.component.molecule.FeatureFlagItem
import net.thunderbird.core.ui.contract.mvi.observe
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun RemoteFeatureFlagScreen(
    isDebugBuild: Boolean,
    onBack: () -> Unit,
    onDebugClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemoteFeatureFlagUiContract.ViewModel = koinViewModel(),
) {
    val snackbarHostState = rememberSnackbarHostState()
    val coroutineScope = rememberCoroutineScope()
    val (stateHolder, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is RemoteFeatureFlagUiContract.Effect.ShowError -> coroutineScope.launch {
                snackbarHostState.showSnackbar(message = getString(effect.error.message))
            }
        }
    }
    val state by stateHolder
    RemoteFeatureFlagScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        isDebugBuild = isDebugBuild,
        onBack = onBack,
        onDebugClick = onDebugClick,
        onEnableChange = { dispatch(RemoteFeatureFlagUiContract.Event.OnEnableChange(enabled = it)) },
        modifier = modifier,
    )
}

@Composable
private fun RemoteFeatureFlagScreen(
    state: RemoteFeatureFlagUiContract.State,
    snackbarHostState: SnackbarHostState,
    isDebugBuild: Boolean,
    onBack: () -> Unit,
    onDebugClick: () -> Unit,
    onEnableChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBarWithBackButton(
                title = stringResource(Res.string.remote_feature_flag_screen_top_bar_title),
                onBackClick = onBack,
            ) {
                if (isDebugBuild) {
                    ButtonIcon(onClick = onDebugClick, imageVector = Icons.Outlined.BugReport)
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
            contentPadding = PaddingValues(
                start = BoltTheme.spacings.default,
                end = BoltTheme.spacings.default,
                bottom = BoltTheme.spacings.triple,
            ),
            modifier = Modifier
                .padding(paddingValues)
                .padding(top = BoltTheme.spacings.triple),
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.clickable(
                        enabled = state.remoteFeatureFlagCatalogAvailable,
                        role = Role.Switch,
                        onClick = { onEnableChange(!state.remoteFeatureFlagEnabled) },
                    ),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        TextTitleMedium(text = stringResource(Res.string.remote_feature_flag_screen_enabled_title))
                        TextBodyMedium(text = stringResource(Res.string.remote_feature_flag_screen_enabled_description))
                    }
                    Switch(
                        checked = state.remoteFeatureFlagEnabled,
                        enabled = state.remoteFeatureFlagCatalogAvailable,
                        onCheckedChange = onEnableChange,
                    )
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.half),
                    modifier = Modifier.padding(vertical = BoltTheme.spacings.default),
                ) {
                    TextTitleMedium(text = stringResource(Res.string.remote_feature_flag_screen_catalog_location))
                    TextBodyMedium(
                        text = buildAnnotatedString {
                            when (val location = state.location) {
                                FeatureFlagLocation.Remote if location.url != null -> {
                                    append(stringResource(location.label))
                                    withLink(LinkAnnotation.Url(location.url)) {
                                        append(" " + location.url)
                                    }
                                }

                                else -> append(stringResource(location.label))
                            }
                        },
                    )
                }
            }
            item {
                TextTitleMedium(
                    text = stringResource(Res.string.remote_feature_flag_screen_enabled_flags),
                    modifier = Modifier.padding(vertical = BoltTheme.spacings.default),
                )
            }

            when {
                state.flags.isEmpty() -> {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(
                                BoltTheme.spacings.double,
                                alignment = Alignment.CenterVertically,
                            ),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(BoltTheme.sizes.large),
                            )
                            TextBodyMedium(text = stringResource(Res.string.remote_feature_flag_screen_no_flags_found))
                        }
                    }
                }

                else -> {
                    items(items = state.flags) { key ->
                        FeatureFlagItem(
                            key = key,
                            overrides = persistentMapOf(),
                            pendingOverrides = persistentMapOf(),
                            flagEnabled = true,
                            isOverridden = false,
                            readonly = true,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

private class RemoteFeatureFlagScreenPrevParamProvider : PreviewParameterProvider<RemoteFeatureFlagUiContract.State> {
    private val allFlags = GeneratedFeatureFlagKey.entries.toImmutableList()

    // Locations follow RemoteFeatureFlagViewModel: runtime overrides win, then remote when enabled, else local.
    private val remote = RemoteFeatureFlagUiContract.State(
        remoteFeatureFlagEnabled = true,
        location = FeatureFlagLocation.Remote,
    )
    private val local = RemoteFeatureFlagUiContract.State(
        remoteFeatureFlagEnabled = false,
        location = FeatureFlagLocation.Local,
    )
    private val runtimeOverride = RemoteFeatureFlagUiContract.State(
        remoteFeatureFlagEnabled = true,
        location = FeatureFlagLocation.RuntimeOverride,
    )

    override val values: Sequence<RemoteFeatureFlagUiContract.State> = sequenceOf(
        remote,
        remote.copy(flags = allFlags),
        local,
        local.copy(flags = allFlags),
        runtimeOverride,
        runtimeOverride.copy(flags = allFlags),
    )
}

@PreviewLightDark
@Composable
private fun Preview(
    @PreviewParameter(RemoteFeatureFlagScreenPrevParamProvider::class) state: RemoteFeatureFlagUiContract.State,
) {
    PreviewWithThemes {
        RemoteFeatureFlagScreen(
            state = state,
            snackbarHostState = rememberSnackbarHostState(),
            isDebugBuild = true,
            onBack = {},
            onDebugClick = {},
            onEnableChange = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

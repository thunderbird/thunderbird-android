package net.thunderbird.feature.debug.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.template.pager.HorizontalTabPagerSecondary
import net.thunderbird.core.ui.compose.designsystem.template.pager.TabSecondaryConfig
import net.thunderbird.feature.debug.settings.featureflag.DebugFeatureFlagSection
import net.thunderbird.feature.debug.settings.navigation.SecretDebugSettingsRoute
import net.thunderbird.feature.debug.settings.notification.DebugNotificationSection
import net.thunderbird.feature.notification.api.ui.InAppNotificationScaffold

@Composable
fun SecretDebugSettingsScreen(
    starterTab: SecretDebugSettingsRoute.Tab,
    onNavigateBack: () -> Unit,
    onFinish: (SecretDebugSettingsRoute.Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isBackNavigationEnabled by remember { mutableStateOf(true) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    InAppNotificationScaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.debug_settings_screen_title),
                navigationIcon = {
                    ButtonIcon(
                        onClick = {
                            if (isBackNavigationEnabled) {
                                onNavigateBack()
                            } else {
                                showUnsavedChangesDialog = true
                            }
                        },
                        imageVector = Icons.Outlined.ArrowBack,
                    )
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        val resources = LocalResources.current
        HorizontalTabPagerSecondary(
            initialSelected = starterTab,
            modifier = Modifier.padding(paddingValues),
        ) {
            pages(
                items = SecretDebugSettingsRoute.Tab.entries.toList(),
                tabConfigBuilder = {
                    TabSecondaryConfig(title = resources.getString(it.titleRes))
                },
            ) { route ->
                when (route) {
                    SecretDebugSettingsRoute.Tab.Notification -> DebugNotificationSection(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(state = rememberScrollState())
                            .padding(MainTheme.spacings.double),
                    )

                    SecretDebugSettingsRoute.Tab.FeatureFlag -> DebugFeatureFlagSection(
                        showUnsavedChangesDialog = showUnsavedChangesDialog,
                        onNavigateBack = onNavigateBack,
                        onFinish = onFinish,
                        onFeatureFlagChange = { pendingChanges ->
                            isBackNavigationEnabled = pendingChanges.isEmpty()
                        },
                        onStayClick = { showUnsavedChangesDialog = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

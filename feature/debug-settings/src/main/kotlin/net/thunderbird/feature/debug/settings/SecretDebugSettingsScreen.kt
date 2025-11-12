package net.thunderbird.feature.debug.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
    modifier: Modifier = Modifier,
) {
    InAppNotificationScaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.debug_settings_screen_title),
                navigationIcon = {
                    ButtonIcon(
                        onClick = onNavigateBack,
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
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}

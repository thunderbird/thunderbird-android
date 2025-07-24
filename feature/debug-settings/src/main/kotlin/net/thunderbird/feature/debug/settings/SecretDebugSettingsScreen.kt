package net.thunderbird.feature.debug.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.debug.settings.notification.DebugNotificationSection

@Composable
fun SecretDebugSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(MainTheme.spacings.double),
        ) {
            DebugNotificationSection()
        }
    }
}

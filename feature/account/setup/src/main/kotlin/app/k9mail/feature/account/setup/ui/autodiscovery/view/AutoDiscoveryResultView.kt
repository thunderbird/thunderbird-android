package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
internal fun AutoDiscoveryResultView(
    settings: AutoDiscoveryResult.Settings?,
    onEditConfigurationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = rememberSaveable {
        mutableStateOf(settings?.isTrusted?.not() ?: false)
    }

    Column(
        modifier = modifier,
    ) {
        Surface(
            shape = MainTheme.shapes.small,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.5f),
                    shape = MainTheme.shapes.small,
                )
                .clickable { expanded.value = !expanded.value },
        ) {
            Column(
                modifier = Modifier.padding(MainTheme.spacings.default),
            ) {
                AutoDiscoveryResultHeaderView(
                    state = if (settings == null) {
                        AutoDiscoveryResultHeaderState.NoSettings
                    } else if (settings.isTrusted) {
                        AutoDiscoveryResultHeaderState.Trusted
                    } else {
                        AutoDiscoveryResultHeaderState.Untrusted
                    },
                    isExpanded = expanded.value,
                )

                if (settings != null) {
                    AnimatedVisibility(visible = expanded.value) {
                        AutoDiscoveryResultBodyView(
                            settings = settings,
                            onEditConfigurationClick = onEditConfigurationClick,
                        )
                    }
                }
            }
        }
    }
}

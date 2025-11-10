package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

@Suppress("LongMethod")
@Composable
internal fun AutoDiscoveryResultHeaderView(
    state: AutoDiscoveryResultHeaderState,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = state.icon,
            tint = selectColor(state),
            modifier = Modifier
                .padding(MainTheme.spacings.default)
                .requiredSize(MainTheme.sizes.medium),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = MainTheme.spacings.default,
                    top = MainTheme.spacings.half,
                    bottom = MainTheme.spacings.half,
                ),
        ) {
            TextTitleLarge(
                text = stringResource(state.titleResourceId),
            )
            TextBodyMedium(
                text = stringResource(state.subtitleResourceId),
            )
        }
        if (state.isExpandable) {
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                modifier = Modifier.padding(MainTheme.spacings.default),
            )
        }
    }
}

@Composable
private fun selectColor(state: AutoDiscoveryResultHeaderState): Color {
    return when (state) {
        AutoDiscoveryResultHeaderState.NoSettings -> MainTheme.colors.primary
        AutoDiscoveryResultHeaderState.Trusted -> MainTheme.colors.success
        AutoDiscoveryResultHeaderState.Untrusted -> MainTheme.colors.warning
    }
}

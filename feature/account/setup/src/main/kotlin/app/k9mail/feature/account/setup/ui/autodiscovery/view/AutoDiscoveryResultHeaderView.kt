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
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge
import net.thunderbird.components.ui.bolt.theme.BoltTheme

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
                .padding(BoltTheme.spacings.default)
                .requiredSize(BoltTheme.sizes.medium),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = BoltTheme.spacings.default,
                    top = BoltTheme.spacings.half,
                    bottom = BoltTheme.spacings.half,
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
                modifier = Modifier.padding(BoltTheme.spacings.default),
            )
        }
    }
}

@Composable
private fun selectColor(state: AutoDiscoveryResultHeaderState): Color {
    return when (state) {
        AutoDiscoveryResultHeaderState.NoSettings -> BoltTheme.colors.primary
        AutoDiscoveryResultHeaderState.Trusted -> BoltTheme.colors.success
        AutoDiscoveryResultHeaderState.Untrusted -> BoltTheme.colors.warning
    }
}

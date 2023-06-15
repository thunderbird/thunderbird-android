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
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.Icon
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody2
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline6
import app.k9mail.core.ui.compose.theme.Icons
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Suppress("LongMethod")
@Composable
internal fun AutoDiscoveryStatusHeaderView(
    state: AutoDiscoveryStatusHeaderState,
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
            TextHeadline6(
                text = stringResource(state.titleResourceId),
            )
            TextBody2(
                text = stringResource(state.subtitleResourceId),
                color = selectColor(state),
            )
        }
        if (state.isExpandable) {
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.expandLess else Icons.Outlined.expandMore,
                modifier = Modifier.padding(MainTheme.spacings.default),
            )
        }
    }
}

@Composable
private fun selectColor(state: AutoDiscoveryStatusHeaderState): Color {
    return when (state) {
        AutoDiscoveryStatusHeaderState.NoSettings -> MainTheme.colors.primary
        AutoDiscoveryStatusHeaderState.Trusted -> MainTheme.colors.success
        AutoDiscoveryStatusHeaderState.Untrusted -> MainTheme.colors.warning
    }
}

@Preview
@Composable
internal fun AutoDiscoveryStatusHeaderViewTrustedCollapsedPreview() {
    PreviewWithThemes {
        AutoDiscoveryStatusHeaderView(
            state = AutoDiscoveryStatusHeaderState.Trusted,
            isExpanded = true,
        )
    }
}

@Preview
@Composable
internal fun AutoDiscoveryStatusHeaderViewTrustedExpandedPreview() {
    PreviewWithThemes {
        AutoDiscoveryStatusHeaderView(
            state = AutoDiscoveryStatusHeaderState.Trusted,
            isExpanded = false,
        )
    }
}

@Preview
@Composable
internal fun AutoDiscoveryStatusHeaderViewUntrustedCollapsedPreview() {
    PreviewWithThemes {
        AutoDiscoveryStatusHeaderView(
            state = AutoDiscoveryStatusHeaderState.Untrusted,
            isExpanded = true,
        )
    }
}

@Preview
@Composable
internal fun AutoDiscoveryStatusHeaderViewUntrustedExpandedPreview() {
    PreviewWithThemes {
        AutoDiscoveryStatusHeaderView(
            state = AutoDiscoveryStatusHeaderState.Untrusted,
            isExpanded = false,
        )
    }
}

@Preview
@Composable
internal fun AutoDiscoveryStatusHeaderNoSettingsPreview() {
    PreviewWithThemes {
        AutoDiscoveryStatusHeaderView(
            state = AutoDiscoveryStatusHeaderState.NoSettings,
            isExpanded = false,
        )
    }
}

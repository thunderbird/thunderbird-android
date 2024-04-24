package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryResultHeaderViewTrustedCollapsedPreview() {
    PreviewWithThemes {
        AutoDiscoveryResultHeaderView(
            state = AutoDiscoveryResultHeaderState.Trusted,
            isExpanded = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryResultHeaderViewTrustedExpandedPreview() {
    PreviewWithThemes {
        AutoDiscoveryResultHeaderView(
            state = AutoDiscoveryResultHeaderState.Trusted,
            isExpanded = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryResultHeaderViewUntrustedCollapsedPreview() {
    PreviewWithThemes {
        AutoDiscoveryResultHeaderView(
            state = AutoDiscoveryResultHeaderState.Untrusted,
            isExpanded = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryResultHeaderViewUntrustedExpandedPreview() {
    PreviewWithThemes {
        AutoDiscoveryResultHeaderView(
            state = AutoDiscoveryResultHeaderState.Untrusted,
            isExpanded = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryResultHeaderNoSettingsPreview() {
    PreviewWithThemes {
        AutoDiscoveryResultHeaderView(
            state = AutoDiscoveryResultHeaderState.NoSettings,
            isExpanded = false,
        )
    }
}

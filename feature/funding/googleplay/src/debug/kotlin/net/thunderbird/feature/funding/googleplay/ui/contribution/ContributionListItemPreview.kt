package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme

@Composable
@Preview(showBackground = true)
internal fun ContributionListItemPreview() {
    PreviewWithTheme {
        ContributionListItem(
            text = "Monthly",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListItemPreviewSelected() {
    PreviewWithTheme {
        ContributionListItem(
            text = "Monthly",
            onClick = {},
            isSelected = true,
        )
    }
}

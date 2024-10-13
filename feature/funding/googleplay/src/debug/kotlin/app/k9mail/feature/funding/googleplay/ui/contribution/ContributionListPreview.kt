package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
internal fun ContributionListPreview() {
    PreviewWithTheme {
        ContributionList(
            contributions = FakeData.recurringContributions,
            isRecurringContributionSelected = true,
            selectedItem = FakeData.recurringContributions.first(),
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListRecurringPreview() {
    PreviewWithTheme {
        ContributionList(
            contributions = FakeData.oneTimeContributions,
            selectedItem = FakeData.oneTimeContributions.last(),
            isRecurringContributionSelected = false,
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
        )
    }
}

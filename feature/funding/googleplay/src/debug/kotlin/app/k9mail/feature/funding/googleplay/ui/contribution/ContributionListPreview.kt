package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
@Preview(showBackground = true)
internal fun ContributionListPreview() {
    PreviewWithTheme {
        ContributionList(
            oneTimeContributions = FakeData.oneTimeContributions,
            recurringContributions = FakeData.recurringContributions,
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
            oneTimeContributions = FakeData.oneTimeContributions,
            recurringContributions = FakeData.recurringContributions,
            selectedItem = FakeData.oneTimeContributions.last(),
            isRecurringContributionSelected = false,
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListOneTimeOnlyPreview() {
    PreviewWithTheme {
        ContributionList(
            oneTimeContributions = FakeData.oneTimeContributions,
            recurringContributions = persistentListOf(),
            selectedItem = null,
            isRecurringContributionSelected = false,
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListRecurringOnlyPreview() {
    PreviewWithTheme {
        ContributionList(
            oneTimeContributions = persistentListOf(),
            recurringContributions = FakeData.recurringContributions,
            selectedItem = null,
            isRecurringContributionSelected = true,
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListEmptyPreview() {
    PreviewWithTheme {
        ContributionList(
            oneTimeContributions = persistentListOf(),
            recurringContributions = persistentListOf(),
            selectedItem = null,
            isRecurringContributionSelected = false,
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
        )
    }
}

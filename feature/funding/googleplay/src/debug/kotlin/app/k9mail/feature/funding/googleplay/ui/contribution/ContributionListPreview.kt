package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.funding.googleplay.domain.DomainContract
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import kotlinx.collections.immutable.persistentListOf

@Composable
@Preview(showBackground = true)
internal fun ContributionListPreview() {
    PreviewWithTheme {
        ContributionList(
            state = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions,
                recurringContributions = FakeData.recurringContributions,
                selectedContribution = FakeData.recurringContributions.first(),
                isRecurringContributionSelected = true,
                isLoading = false,
            ),
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListRecurringPreview() {
    PreviewWithTheme {
        ContributionList(
            state = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions,
                recurringContributions = FakeData.recurringContributions,
                selectedContribution = FakeData.oneTimeContributions.last(),
                isRecurringContributionSelected = false,
                isLoading = false,
            ),
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListOneTimeOnlyPreview() {
    PreviewWithTheme {
        ContributionList(
            state = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions,
                recurringContributions = persistentListOf(),
                selectedContribution = null,
                isRecurringContributionSelected = false,
                isLoading = false,
            ),
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListRecurringOnlyPreview() {
    PreviewWithTheme {
        ContributionList(
            state = ContributionListState(
                oneTimeContributions = persistentListOf(),
                recurringContributions = FakeData.recurringContributions,
                selectedContribution = null,
                isRecurringContributionSelected = true,
                isLoading = false,
            ),
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListEmptyPreview() {
    PreviewWithTheme {
        ContributionList(
            state = ContributionListState(
                oneTimeContributions = persistentListOf(),
                recurringContributions = persistentListOf(),
                selectedContribution = null,
                isRecurringContributionSelected = false,
                isLoading = false,
            ),
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListLoadingPreview() {
    PreviewWithTheme {
        ContributionList(
            state = ContributionListState(
                oneTimeContributions = persistentListOf(),
                recurringContributions = persistentListOf(),
                selectedContribution = null,
                isRecurringContributionSelected = false,
                isLoading = true,
            ),
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListErrorPreview() {
    PreviewWithTheme {
        ContributionList(
            state = ContributionListState(
                oneTimeContributions = persistentListOf(),
                recurringContributions = persistentListOf(),
                selectedContribution = null,
                isRecurringContributionSelected = false,
                isLoading = false,
                error = DomainContract.BillingError.UnknownError(
                    "An error occurred",
                ),
            ),
            onOneTimeContributionTypeClick = {},
            onRecurringContributionTypeClick = {},
            onItemClick = {},
            onRetryClick = {},
        )
    }
}

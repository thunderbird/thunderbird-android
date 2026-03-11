package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState

@Composable
@Preview(showBackground = true)
internal fun ContributionListPreview() {
    PreviewWithTheme {
        ContributionList(
            state = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions,
                recurringContributions = FakeData.recurringContributions,
                selectedContributionId = FakeData.recurringContributions.first().id,
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
                selectedContributionId = FakeData.oneTimeContributions.last().id,
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
                selectedContributionId = null,
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
                selectedContributionId = null,
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
                selectedContributionId = null,
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
                selectedContributionId = null,
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
                selectedContributionId = null,
                isRecurringContributionSelected = false,
                isLoading = false,
                error = FundingDomainContract.ContributionError.UnknownError(
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

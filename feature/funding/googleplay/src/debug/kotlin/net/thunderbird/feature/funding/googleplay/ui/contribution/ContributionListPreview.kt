package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionList
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.ContributionType
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.State

@Composable
@Preview(showBackground = true)
internal fun ContributionListRecurringPreview() {
    PreviewWithTheme {
        ContributionList(
            state = State(
                contributions = AvailableContributions(
                    oneTimeContributions = FakeData.oneTimeContributions,
                    recurringContributions = FakeData.recurringContributions,
                    preselection = FakeData.preselection,
                ),
                selectedType = ContributionType.Recurring,
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListOneTimePreview() {
    PreviewWithTheme {
        ContributionList(
            state = State(
                contributions = AvailableContributions(
                    oneTimeContributions = FakeData.oneTimeContributions,
                    recurringContributions = FakeData.recurringContributions,
                    preselection = FakeData.preselection,
                ),
                selectedType = ContributionType.OneTime,
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListOneTimeOnlyPreview() {
    PreviewWithTheme {
        ContributionList(
            state = State(
                contributions = AvailableContributions(
                    oneTimeContributions = FakeData.oneTimeContributions,
                    recurringContributions = persistentListOf(),
                    preselection = FakeData.preselection.copy(
                        recurringId = null,
                    ),
                ),
                selectedType = ContributionType.OneTime,
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListRecurringOnlyPreview() {
    PreviewWithTheme {
        ContributionList(
            state = State(
                contributions = AvailableContributions(
                    oneTimeContributions = persistentListOf(),
                    recurringContributions = FakeData.recurringContributions,
                    preselection = FakeData.preselection.copy(
                        oneTimeId = null,
                    ),
                ),
                selectedType = ContributionType.Recurring,
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListEmptyPreview() {
    PreviewWithTheme {
        ContributionList(
            state = State(
                contributions = AvailableContributions.Empty,
                selectedType = ContributionType.Recurring,
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListLoadingPreview() {
    PreviewWithTheme {
        ContributionList(
            state = State(
                contributions = AvailableContributions.Empty,
                selectedType = ContributionType.Recurring,
                isLoading = true,
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionListErrorPreview() {
    PreviewWithTheme {
        ContributionList(
            state = State(
                contributions = AvailableContributions.Empty,
                selectedType = ContributionType.Recurring,
                isLoading = false,
                error = FundingDomainContract.ContributionError.UnknownError(
                    "An error occurred",
                ),
            ),
            onEvent = {},
        )
    }
}

package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State

@Composable
@Preview(showBackground = true)
fun ContributionContentPreview() {
    PreviewWithTheme {
        ContributionContent(
            state = State(
                listState = ContributionListState(
                    recurringContributions = FakeData.recurringContributions,
                    oneTimeContributions = FakeData.oneTimeContributions,
                    selectedContribution = FakeData.recurringContributions.first(),
                    isLoading = false,
                ),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionContentEmptyPreview() {
    PreviewWithTheme {
        ContributionContent(
            state = State(
                listState = ContributionListState(
                    isLoading = false,
                ),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionContentLoadingPreview() {
    PreviewWithTheme {
        ContributionContent(
            state = State(
                listState = ContributionListState(
                    isLoading = true,
                ),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionContentListErrorPreview() {
    PreviewWithTheme {
        ContributionContent(
            state = State(
                listState = ContributionListState(
                    error = FundingDomainContract.ContributionError.DeveloperError("Developer error"),
                    isLoading = false,
                ),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionContentPurchaseErrorPreview() {
    PreviewWithTheme {
        ContributionContent(
            state = State(
                purchaseError = FundingDomainContract.ContributionError.DeveloperError("Developer error"),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

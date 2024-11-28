package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.funding.googleplay.domain.DomainContract
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State

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
                    error = DomainContract.BillingError.DeveloperError("Developer error"),
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
                purchaseError = DomainContract.BillingError.DeveloperError("Developer error"),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

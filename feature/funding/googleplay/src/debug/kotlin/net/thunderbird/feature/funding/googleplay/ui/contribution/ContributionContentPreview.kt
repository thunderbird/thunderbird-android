package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.State as ListState
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.State as PurchaseState

@Composable
@Preview(showBackground = true)
fun ContributionContentPreview() {
    PreviewWithTheme {
        ContributionContent(
            state = State(),
            listState = ListState(
                contributions = AvailableContributions(
                    recurringContributions = FakeData.recurringContributions,
                    oneTimeContributions = FakeData.oneTimeContributions,
                    preselection = FakeData.preselection,
                ),
                isLoading = false,
            ),
            purchaseState = PurchaseState(),
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
            state = State(),
            listState = ListState(
                isLoading = false,
            ),
            purchaseState = PurchaseState(),
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
            state = State(),
            listState = ListState(
                isLoading = true,
            ),
            purchaseState = PurchaseState(),
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
            state = State(),
            listState = ListState(
                error = FundingDomainContract.ContributionError.DeveloperError("Developer error"),
                isLoading = false,
            ),
            purchaseState = PurchaseState(),
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
            state = State(),
            listState = ListState(
                isLoading = false,
            ),
            purchaseState = PurchaseState(
                purchaseFlow = PurchaseSliceContract.PurchaseFlow.Failed(
                    contributionId = FakeData.recurringContributions.first().id,
                    error = FundingDomainContract.ContributionError.DeveloperError("Developer error"),
                ),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

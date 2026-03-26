package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.PurchaseFlow
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.State

@Composable
@Preview(showBackground = true)
fun ContributionFooterPreview() {
    PreviewWithTheme {
        ContributionFooter(
            state = State(
                purchasedContribution = null,
                purchaseFlow = PurchaseFlow.Idle,
            ),
            onEvent = {},
            onShowContributionListClick = {},
            selectedContributionId = ContributionId("contributionId"),
            isContributionListShown = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterPurchasingPreview() {
    PreviewWithTheme {
        ContributionFooter(
            state = State(
                purchasedContribution = null,
                purchaseFlow = PurchaseFlow.Launching(ContributionId("contributionId")),
            ),
            onEvent = {},
            onShowContributionListClick = {},
            selectedContributionId = ContributionId("contributionId"),
            isContributionListShown = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterDisabledPreview() {
    PreviewWithTheme {
        ContributionFooter(
            state = State(
                purchasedContribution = null,
                purchaseFlow = PurchaseFlow.Idle,
            ),
            onEvent = {},
            onShowContributionListClick = {},
            selectedContributionId = null,
            isContributionListShown = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterWithRecurringContributionPreview() {
    PreviewWithTheme {
        ContributionFooter(
            state = State(
                purchasedContribution = FakeData.purchasedRecurringContribution,
                purchaseFlow = PurchaseFlow.Idle,
            ),
            onEvent = {},
            onShowContributionListClick = {},
            selectedContributionId = ContributionId("contributionId"),
            isContributionListShown = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterWithOneTimeContributionPreview() {
    PreviewWithTheme {
        ContributionFooter(
            state = State(
                purchasedContribution = FakeData.purchasedOneTimeContribution,
                purchaseFlow = PurchaseFlow.Idle,
            ),
            onEvent = {},
            onShowContributionListClick = {},
            selectedContributionId = ContributionId("contributionId"),
            isContributionListShown = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterWithOneTimeContributionAndListPreview() {
    PreviewWithTheme {
        ContributionFooter(
            state = State(
                purchasedContribution = FakeData.purchasedOneTimeContribution,
                purchaseFlow = PurchaseFlow.Idle,
            ),
            onEvent = {},
            onShowContributionListClick = {},
            selectedContributionId = ContributionId("contributionId"),
            isContributionListShown = true,
        )
    }
}

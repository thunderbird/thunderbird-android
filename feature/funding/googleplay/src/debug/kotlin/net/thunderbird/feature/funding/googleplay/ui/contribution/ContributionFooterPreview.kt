package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
fun ContributionFooterPreview() {
    PreviewWithTheme {
        ContributionFooter(
            onPurchaseClick = {},
            onCancelPurchaseClick = {},
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            purchasedContribution = null,
            isPurchaseEnabled = true,
            isPurchasing = false,
            isContributionListShown = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterPurchasingPreview() {
    PreviewWithTheme {
        ContributionFooter(
            onPurchaseClick = {},
            onCancelPurchaseClick = {},
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            purchasedContribution = null,
            isPurchaseEnabled = true,
            isPurchasing = true,
            isContributionListShown = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterDisabledPreview() {
    PreviewWithTheme {
        ContributionFooter(
            purchasedContribution = null,
            onPurchaseClick = {},
            onCancelPurchaseClick = {},
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            isContributionListShown = false,
            isPurchaseEnabled = false,
            isPurchasing = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterWithRecurringContributionPreview() {
    PreviewWithTheme {
        ContributionFooter(
            purchasedContribution = FakeData.recurringContribution,
            onPurchaseClick = {},
            onCancelPurchaseClick = {},
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            isPurchaseEnabled = true,
            isPurchasing = false,
            isContributionListShown = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterWithOneTimeContributionPreview() {
    PreviewWithTheme {
        ContributionFooter(
            purchasedContribution = FakeData.oneTimeContribution,
            onPurchaseClick = {},
            onCancelPurchaseClick = {},
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            isPurchaseEnabled = true,
            isPurchasing = false,
            isContributionListShown = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionFooterWithOneTimeContributionAndListPreview() {
    PreviewWithTheme {
        ContributionFooter(
            purchasedContribution = FakeData.oneTimeContribution,
            onPurchaseClick = {},
            onCancelPurchaseClick = {},
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            isPurchaseEnabled = true,
            isPurchasing = false,
            isContributionListShown = true,
        )
    }
}

package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
fun ContributionFooterPreview() {
    PreviewWithTheme {
        ContributionFooter(
            onPurchaseClick = {},
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            purchasedContribution = null,
            isPurchaseEnabled = true,
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
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            isContributionListShown = false,
            isPurchaseEnabled = false,
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
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            isPurchaseEnabled = true,
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
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            isPurchaseEnabled = true,
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
            onManagePurchaseClick = {},
            onShowContributionListClick = {},
            isPurchaseEnabled = true,
            isContributionListShown = true,
        )
    }
}

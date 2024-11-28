package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
internal fun ContributionHeaderPreview() {
    PreviewWithTheme {
        ContributionHeader(purchasedContribution = null)
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionHeaderWithPurchasedOneTimeContributionPreview() {
    PreviewWithTheme {
        ContributionHeader(
            purchasedContribution = FakeData.oneTimeContribution,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionHeaderWithPurchasedRecurringContributionPreview() {
    PreviewWithTheme {
        ContributionHeader(
            purchasedContribution = FakeData.recurringContribution,
        )
    }
}

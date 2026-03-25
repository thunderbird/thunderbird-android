package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.datetime.LocalDateTime
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution

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
            purchasedContribution = PurchasedContribution(
                id = FakeData.oneTimeContribution.id,
                contribution = FakeData.oneTimeContribution,
                purchaseDate = LocalDateTime(2024, 6, 1, 12, 0),
            ),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContributionHeaderWithPurchasedRecurringContributionPreview() {
    PreviewWithTheme {
        ContributionHeader(
            purchasedContribution = PurchasedContribution(
                id = FakeData.recurringContribution.id,
                contribution = FakeData.recurringContribution,
                purchaseDate = LocalDateTime(2024, 6, 1, 12, 0),
            ),
        )
    }
}

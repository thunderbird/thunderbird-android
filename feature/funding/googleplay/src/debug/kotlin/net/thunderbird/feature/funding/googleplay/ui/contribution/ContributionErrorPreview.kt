package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError

@Composable
@Preview(showBackground = true)
fun ContributionErrorPurchaseFailedPreview() {
    PreviewWithTheme {
        ContributionError(
            error = ContributionError.PurchaseFailed("Purchase failed"),
            onDismissClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionErrorServiceDisconnectedPreview() {
    PreviewWithTheme {
        ContributionError(
            error = ContributionError.ServiceDisconnected("Service disconnected"),
            onDismissClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionErrorUnknownErrorPreview() {
    PreviewWithTheme {
        ContributionError(
            error = ContributionError.DeveloperError("Unknown error"),
            onDismissClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionErrorDeveloperErrorPreview() {
    PreviewWithTheme {
        ContributionError(
            error = ContributionError.UserCancelled("User cancelled"),
            onDismissClick = {},
        )
    }
}

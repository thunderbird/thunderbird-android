package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError

@Composable
@Preview(showBackground = true)
fun ContributionErrorPurchaseFailedPreview() {
    PreviewWithTheme {
        ContributionError(
            error = BillingError.PurchaseFailed("Purchase failed"),
            onDismissClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionErrorServiceDisconnectedPreview() {
    PreviewWithTheme {
        ContributionError(
            error = BillingError.ServiceDisconnected("Service disconnected"),
            onDismissClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionErrorUnknownErrorPreview() {
    PreviewWithTheme {
        ContributionError(
            error = BillingError.DeveloperError("Unknown error"),
            onDismissClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionErrorDeveloperErrorPreview() {
    PreviewWithTheme {
        ContributionError(
            error = BillingError.UserCancelled("User cancelled"),
            onDismissClick = {},
        )
    }
}

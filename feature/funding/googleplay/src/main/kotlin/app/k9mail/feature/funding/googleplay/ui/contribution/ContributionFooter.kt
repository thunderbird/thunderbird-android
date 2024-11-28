package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.feature.funding.googleplay.R
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution

@Composable
internal fun ContributionFooter(
    purchasedContribution: Contribution?,
    onPurchaseClick: () -> Unit,
    onManagePurchaseClick: (Contribution) -> Unit,
    onShowContributionListClick: () -> Unit,
    isPurchaseEnabled: Boolean,
    isContributionListShown: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        if (purchasedContribution != null && !isContributionListShown) {
            when (purchasedContribution) {
                is RecurringContribution -> {
                    ButtonFilled(
                        text = stringResource(
                            R.string.funding_googleplay_contribution_footer_manage_button,
                        ),
                        onClick = { onManagePurchaseClick(purchasedContribution) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is OneTimeContribution -> {
                    ButtonFilled(
                        text = stringResource(
                            R.string.funding_googleplay_contribution_footer_show_contribution_list_button,
                        ),
                        onClick = onShowContributionListClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            ButtonFilled(
                text = stringResource(
                    if (isPurchaseEnabled) {
                        R.string.funding_googleplay_contribution_footer_payment_button
                    } else {
                        R.string.funding_googleplay_contribution_footer_payment_unavailable_button
                    },
                ),
                onClick = onPurchaseClick,
                enabled = isPurchaseEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

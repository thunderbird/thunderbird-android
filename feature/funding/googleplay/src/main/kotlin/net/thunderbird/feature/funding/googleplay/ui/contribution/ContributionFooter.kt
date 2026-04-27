package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import net.thunderbird.core.ui.compose.theme2.LocalContentColor
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.funding.googleplay.R
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.State

@SuppressWarnings(
    "LongParameterList",
    "LongMethod",
)
@Composable
internal fun ContributionFooter(
    state: State,
    onEvent: (Event) -> Unit,
    onShowContributionListClick: () -> Unit,
    selectedContributionId: ContributionId?,
    isContributionListShown: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        if (state.purchasedContribution != null && !isContributionListShown) {
            when (state.purchasedContribution.contribution) {
                is RecurringContribution -> {
                    ButtonFilled(
                        text = stringResource(
                            R.string.funding_googleplay_contribution_footer_manage_button,
                        ),
                        onClick = {
                            onEvent(Event.ManagePurchaseClicked(state.purchasedContribution.id))
                        },
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
        } else if (state.purchaseFlow is PurchaseSliceContract.PurchaseFlow.Launching ||
            state.purchaseFlow is PurchaseSliceContract.PurchaseFlow.Waiting
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(MainTheme.sizes.icon),
                        color = LocalContentColor.current,
                    )
                    Spacer(modifier = Modifier.width(MainTheme.spacings.double))
                    TextBodyMedium(
                        text = stringResource(
                            R.string.funding_googleplay_contribution_footer_payment_processing_button,
                        ),
                        color = LocalContentColor.current,
                    )
                }
                ButtonText(
                    text = stringResource(R.string.funding_googleplay_contribution_footer_payment_cancel_button),
                    onClick = { onEvent(Event.CancelPurchaseClicked) },
                )
            }
        } else {
            ButtonFilled(
                text = stringResource(
                    if (selectedContributionId != null) {
                        R.string.funding_googleplay_contribution_footer_payment_button
                    } else {
                        R.string.funding_googleplay_contribution_footer_payment_unavailable_button
                    },
                ),
                onClick = { selectedContributionId?.let { id -> onEvent(Event.PurchaseClicked(id)) } },
                enabled = selectedContributionId != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

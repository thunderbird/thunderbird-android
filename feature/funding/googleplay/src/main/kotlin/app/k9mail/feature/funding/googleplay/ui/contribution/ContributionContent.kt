package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State

@Composable
internal fun ContributionContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ResponsiveWidthContainer(
        modifier = modifier
            .testTag("ContributionContent")
            .padding(contentPadding),
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MainTheme.spacings.quadruple)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.triple),
        ) {
            ContributionHeader(
                purchasedContribution = state.purchasedContribution,
            )

            if (state.showContributionList) {
                ContributionList(
                    state = state.listState,
                    onOneTimeContributionTypeClick = {
                        onEvent(Event.OnOneTimeContributionSelected)
                    },
                    onRecurringContributionTypeClick = {
                        onEvent(Event.OnRecurringContributionSelected)
                    },
                    onItemClick = {
                        onEvent(Event.OnContributionItemClicked(it))
                    },
                    onRetryClick = {
                        onEvent(Event.OnRetryClicked)
                    },
                )
            }

            if (state.purchaseError != null) {
                ContributionError(
                    error = state.purchaseError,
                    onDismissClick = { onEvent(Event.OnDismissPurchaseErrorClicked) },
                )
            }

            ContributionFooter(
                purchasedContribution = state.purchasedContribution,
                onPurchaseClick = { onEvent(Event.OnPurchaseClicked) },
                onManagePurchaseClick = { onEvent(Event.OnManagePurchaseClicked(it)) },
                onShowContributionListClick = { onEvent(Event.OnShowContributionListClicked) },
                isPurchaseEnabled = state.listState.selectedContribution != null,
                isContributionListShown = state.showContributionList,
            )
        }
    }
}

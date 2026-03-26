package net.thunderbird.feature.funding.googleplay.ui.contribution

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
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionList
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.State as ListState
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.Event as PurchaseEvent
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.State as PurchaseState

@Composable
internal fun ContributionContent(
    state: State,
    listState: ListState,
    purchaseState: PurchaseState,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ResponsiveWidthContainer(
        modifier = modifier
            .testTagAsResourceId("ContributionContent")
            .padding(contentPadding),
    ) { contentPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MainTheme.spacings.quadruple)
                .verticalScroll(scrollState)
                .padding(contentPadding),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.triple),
        ) {
            ContributionHeader(
                purchasedContribution = purchaseState.purchasedContribution,
            )

            if (state.showContributionList) {
                ContributionList(
                    state = listState,
                    onEvent = { onEvent(Event.List(it)) },
                )
            }

            if (purchaseState.purchaseFlow is PurchaseSliceContract.PurchaseFlow.Failed) {
                ContributionError(
                    error = purchaseState.purchaseFlow.error,
                    onDismissClick = {
                        onEvent(Event.Purchase(PurchaseEvent.DismissPurchaseErrorClicked))
                    },
                )
            }

            ContributionFooter(
                state = purchaseState,
                onEvent = { onEvent(Event.Purchase(it)) },
                onShowContributionListClick = {
                    onEvent(Event.ShowContributionListClicked)
                },
                selectedContributionId = state.selectedContributionId,
                isContributionListShown = state.showContributionList,
            )
        }
    }
}

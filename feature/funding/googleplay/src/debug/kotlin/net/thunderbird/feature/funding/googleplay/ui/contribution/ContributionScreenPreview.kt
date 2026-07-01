package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevicesWithBackground
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.State as ListState
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.State as PurchaseState

@Composable
@PreviewDevicesWithBackground
fun ContributionScreenPreview() {
    PreviewWithTheme {
        ContributionScreen(
            onBack = {},
            viewModel = viewModel {
                FakeContributionViewModel(
                    initialState = State(),
                    listState = MutableStateFlow(ListState()),
                    purchaseState = MutableStateFlow(PurchaseState()),
                )
            },
        )
    }
}

package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.coroutines.flow.MutableStateFlow
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

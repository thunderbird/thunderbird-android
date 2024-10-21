package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State

@Composable
@PreviewDevicesWithBackground
fun ContributionScreenPreview() {
    PreviewWithTheme {
        ContributionScreen(
            onBack = {},
            viewModel = FakeContributionViewModel(
                initialState = State(
                    listState = ContributionListState(
                        recurringContributions = FakeData.recurringContributions,
                        oneTimeContributions = FakeData.oneTimeContributions,
                        selectedContribution = FakeData.recurringContributions.first(),
                    ),
                ),
            ),
        )
    }
}

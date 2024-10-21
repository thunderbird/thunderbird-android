package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.testing.BaseFakeViewModel
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel

internal class FakeContributionViewModel(
    initialState: State = State(),
) : BaseFakeViewModel<State, Event, Effect>(initialState), ViewModel

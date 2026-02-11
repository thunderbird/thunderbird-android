package net.thunderbird.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.testing.BaseFakeViewModel
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel

internal class FakeContributionViewModel(
    initialState: State = State(),
) : BaseFakeViewModel<State, Event, Effect>(initialState), ViewModel

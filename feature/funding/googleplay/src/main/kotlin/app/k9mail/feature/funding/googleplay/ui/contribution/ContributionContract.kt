package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class ContributionContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Nothing>

    @Stable
    data class State(
        val recurringContributions: ImmutableList<RecurringContribution> = persistentListOf(),
        val oneTimeContributions: ImmutableList<OneTimeContribution> = persistentListOf(),
        val selectedContribution: Contribution? = null,
        val isRecurringContributionSelected: Boolean = false,
    )

    sealed interface Event {
        data object OnOneTimeContributionClicked : Event
        data object OnRecurringContributionClicked : Event
        data class OnContributionClicked(val item: Contribution) : Event
        data object OnPurchaseClicked : Event
    }
}

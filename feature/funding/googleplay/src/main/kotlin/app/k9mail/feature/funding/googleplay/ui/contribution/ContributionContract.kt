package app.k9mail.feature.funding.googleplay.ui.contribution

import android.app.Activity
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
        val oneTimeContributions: ImmutableList<OneTimeContribution> = persistentListOf(),
        val recurringContributions: ImmutableList<RecurringContribution> = persistentListOf(),
        val purchasedContribution: Contribution? = null,
        val selectedContribution: Contribution? = null,
        val isRecurringContributionSelected: Boolean = false,
    )

    sealed interface Event {
        data object OnOneTimeContributionSelected : Event
        data object OnRecurringContributionSelected : Event

        data class OnContributionItemClicked(
            val item: Contribution,
        ) : Event

        data class OnPurchaseClicked(
            val activity: Activity,
        ) : Event

        data class OnManagePurchaseClicked(
            val contribution: Contribution,
        ) : Event
    }
}

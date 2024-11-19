package app.k9mail.feature.funding.googleplay.ui.contribution

import android.app.Activity
import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingErrorState
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class ContributionContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val listState: ContributionListState = ContributionListState(),
        val purchasedContribution: Contribution? = null,

        val showContributionList: Boolean = true,
        val showRecurringContributions: Boolean = false,

        val purchaseError: BillingError? = null,
    )

    @Stable
    data class ContributionListState(
        val oneTimeContributions: ImmutableList<OneTimeContribution> = persistentListOf(),
        val recurringContributions: ImmutableList<RecurringContribution> = persistentListOf(),
        val selectedContribution: Contribution? = null,
        val isRecurringContributionSelected: Boolean = true,

        override val error: BillingError? = null,
        override val isLoading: Boolean = true,
    ) : LoadingErrorState<BillingError>

    sealed interface Event {
        data object OnOneTimeContributionSelected : Event
        data object OnRecurringContributionSelected : Event

        data object OnShowContributionListClicked : Event

        data class OnContributionItemClicked(
            val item: Contribution,
        ) : Event

        data object OnPurchaseClicked : Event

        data class OnManagePurchaseClicked(
            val contribution: Contribution,
        ) : Event

        data object OnDismissPurchaseErrorClicked : Event

        data object OnRetryClicked : Event
    }

    sealed interface Effect {
        data class PurchaseContribution(
            val startPurchaseFlow: (Activity) -> Unit,
        ) : Effect

        data class ManageSubscription(
            val productId: String,
        ) : Effect
    }
}

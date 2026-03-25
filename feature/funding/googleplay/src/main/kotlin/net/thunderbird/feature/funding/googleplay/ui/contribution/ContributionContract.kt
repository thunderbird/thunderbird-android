package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingErrorState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionPreselection
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class ContributionContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val listState: ContributionListState = ContributionListState(),
        val purchasedContribution: PurchasedContribution? = null,

        val showContributionList: Boolean = true,
        val showRecurringContributions: Boolean = false,

        val isPurchasing: Boolean = false,
        val purchaseError: ContributionError? = null,
    )

    @Stable
    data class ContributionListState(
        val oneTimeContributions: ImmutableList<OneTimeContribution> = persistentListOf(),
        val recurringContributions: ImmutableList<RecurringContribution> = persistentListOf(),
        val preselection: ContributionPreselection = ContributionPreselection(null, null),
        val selectedContributionId: ContributionId? = null,
        val isRecurringContributionSelected: Boolean = true,
        val contributionTypes: ImmutableList<ContributionType> = persistentListOf(
            ContributionType.OneTime,
            ContributionType.Recurring,
        ),

        override val error: ContributionError? = null,
        override val isLoading: Boolean = true,
    ) : LoadingErrorState<ContributionError>

    enum class ContributionType {
        OneTime,
        Recurring,
    }

    sealed interface Event {
        data class OnContributionTypeSelected(
            val type: ContributionType,
        ) : Event

        data object OnShowContributionListClicked : Event

        data class OnContributionItemClicked(
            val contributionId: ContributionId,
        ) : Event

        data object OnPurchaseClicked : Event
        data object OnCancelPurchaseClicked : Event

        data class OnManagePurchaseClicked(
            val contribution: Contribution,
        ) : Event

        data object OnDismissPurchaseErrorClicked : Event

        data object OnRetryClicked : Event
    }

    sealed interface Effect {
        data class ManageSubscription(
            val productId: String,
        ) : Effect
    }
}

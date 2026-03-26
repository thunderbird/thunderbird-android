package net.thunderbird.feature.funding.googleplay.ui.contribution.list

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingErrorState
import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.ui.contract.udf.UnidirectionalSlice
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId

internal interface ContributionListSliceContract {

    interface Slice : UnidirectionalSlice<State, Event, Effect> {
        interface Factory {
            fun create(scope: CoroutineScope): Slice
        }
    }

    @Stable
    data class State(
        val contributions: AvailableContributions = AvailableContributions.Empty,
        val selectedType: ContributionType = ContributionType.Recurring,
        val selectedContribution: Contribution? = null,
        override val error: ContributionError? = null,
        override val isLoading: Boolean = true,
    ) : LoadingErrorState<ContributionError>

    enum class ContributionType {
        OneTime,
        Recurring,
    }

    sealed interface Event {
        data class TypeClicked(val type: ContributionType) : Event
        data class ItemClicked(val contributionId: ContributionId) : Event
        data object RetryClicked : Event
    }

    sealed interface Effect {
        data class SelectionChanged(val contributionId: ContributionId?) : Effect
    }
}

package net.thunderbird.feature.funding.googleplay.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfig
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionPurchase
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError

internal class LocalContributionPurchaseDataSource(
    private val configStore: Local.ContributionConfigStore,
) : Local.ContributionPurchaseDataSource {

    override fun get(): Flow<Outcome<ContributionPurchase?, ContributionError>> {
        return configStore.config.map {
            Outcome.success(it.lastPurchasedContribution)
        }
    }

    override suspend fun update(purchase: ContributionPurchase): Outcome<Unit, ContributionError> {
        return Outcome.success(
            configStore.update { current ->
                (current ?: ContributionConfig.DEFAULT).copy(lastPurchasedContribution = purchase)
            },
        )
    }
}

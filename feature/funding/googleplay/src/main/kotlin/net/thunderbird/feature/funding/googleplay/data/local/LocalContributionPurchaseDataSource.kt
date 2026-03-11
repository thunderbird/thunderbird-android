package net.thunderbird.feature.funding.googleplay.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Mapper.ContributionPurchase
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfig
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution

internal class LocalContributionPurchaseDataSource(
    private val configStore: Local.ContributionConfigStore,
    private val contributionPurchaseMapper: ContributionPurchase,
) : Local.ContributionPurchaseDataSource {

    override fun get(): Flow<Outcome<PurchasedContribution?, ContributionError>> {
        return configStore.config.map {
            Outcome.success(it.lastPurchasedContribution?.let(contributionPurchaseMapper::mapToPurchasedContribution))
        }
    }

    override suspend fun update(purchase: PurchasedContribution): Outcome<Unit, ContributionError> {
        return Outcome.success(
            configStore.update { current ->
                (current ?: ContributionConfig.DEFAULT).copy(
                    lastPurchasedContribution = contributionPurchaseMapper.mapToContributionPurchase(purchase),
                )
            },
        )
    }
}

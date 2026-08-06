package net.thunderbird.feature.funding.googleplay.data.mapper

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Mapper
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionPurchase
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution

internal class ContributionPurchaseMapper : Mapper.ContributionPurchase {

    override fun mapToContributionPurchase(purchasedContribution: PurchasedContribution): ContributionPurchase {
        val contribution = purchasedContribution.contribution as OneTimeContribution
        return ContributionPurchase(
            id = purchasedContribution.id.value,
            title = contribution.title,
            description = contribution.description,
            price = contribution.price,
            priceFormatted = contribution.priceFormatted,
            productId = contribution.id.value,
            orderId = null,
            purchaseTimeMillis = purchasedContribution.purchaseDate
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds(),
        )
    }

    override fun mapToPurchasedContribution(contributionPurchase: ContributionPurchase): PurchasedContribution =
        PurchasedContribution(
            id = ContributionId(contributionPurchase.id),
            contribution = OneTimeContribution(
                id = ContributionId(contributionPurchase.productId),
                title = contributionPurchase.title,
                description = contributionPurchase.description,
                price = contributionPurchase.price,
                priceFormatted = contributionPurchase.priceFormatted,
            ),
            purchaseDate = Instant.fromEpochMilliseconds(
                contributionPurchase.purchaseTimeMillis,
            ).toLocalDateTime(TimeZone.currentSystemDefault()),
        )
}

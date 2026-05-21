package net.thunderbird.feature.funding.googleplay.domain.entity

import kotlinx.datetime.LocalDateTime

internal data class PurchasedContribution(
    val id: ContributionId,
    val contribution: Contribution,
    val purchaseDate: LocalDateTime,
)

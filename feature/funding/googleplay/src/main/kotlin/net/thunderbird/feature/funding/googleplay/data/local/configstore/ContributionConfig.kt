package net.thunderbird.feature.funding.googleplay.data.local.configstore

import kotlinx.serialization.Serializable

/**
 * Contribution config definition.
 *
 * @property lastPurchasedContribution The last purchased contribution, if any
 */
@Serializable
internal data class ContributionConfig(
    val lastPurchasedContribution: ContributionPurchase?,
) {
    companion object {
        val DEFAULT = ContributionConfig(
            lastPurchasedContribution = null,
        )
    }
}
